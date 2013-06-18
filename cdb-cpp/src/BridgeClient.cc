/**
 * the-lift.cpp
 * Copyright (c) 2013 Fred Eisele (phreed at gmail dot com)
 *
 * Distributed under the Boost Software License, Version 1.0. (See accompanying
 * file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
 */

#include "BridgeClient.h"
#include "FramedPayload.h"
#include "CdbMsg.pb.h"

#include <cassert>
#include <iostream>
#include <map>
#include <deque>
#include <string>
#include <sstream>
#include <vector>
#include <boost/cstdint.hpp>
#include <boost/enable_shared_from_this.hpp>
// #include <boost/log/trivial.hpp>

namespace asio = boost::asio;
using asio::ip::tcp;
using boost::uint8_t;

#define DEBUG true

namespace meta = edu::vanderbilt::isis::meta;
typedef std::deque<meta::Control> edit_deque;

/**
 * connection - handles a connection with a single client.
 * Create only through the BridgeConnection::create factory.
 * <p>
 * As messages are received they are placed in a queue which
 * are then passed to the CREO api to update the model.
 */
class BridgeConnection : public boost::enable_shared_from_this<BridgeConnection> {
public:
	typedef boost::shared_ptr<BridgeConnection> Pointer;
	typedef boost::shared_ptr<meta::Control> ControlPointer;

	/**
	 * The factory function to create the bridge connection object.
	 */
	static Pointer create(asio::io_service& io_service, edit_deque& edit) {
		return Pointer(new BridgeConnection(io_service, edit));
	}

	tcp::socket& get_socket() {
		return m_socket;
	}

	void start(boost::asio::ip::tcp::resolver::iterator it) {
		m_socket.async_connect(*it, 
			boost::bind(&BridgeConnection::handle_connect,
						shared_from_this(), asio::placeholders::error));
	}

private:
	tcp::socket m_socket;
	edit_deque& m_edit_ref;
	std::vector<uint8_t> m_readbuf;
	FramedPayload<meta::Control> m_framed_control;

	BridgeConnection(asio::io_service& io_service, edit_deque& edit) :
			m_socket(io_service), m_edit_ref(edit), m_framed_control(
					boost::shared_ptr<meta::Control>(new meta::Control())) {
	}

	void handle_connect(const boost::system::error_code& error) {
		if (error) {
			return;
		}
		start_read_header();
	}

	void start_read_header() {
		m_readbuf.resize(HEADER_SIZE);
		asio::async_read(m_socket, asio::buffer(m_readbuf),
				boost::bind(&BridgeConnection::handle_read_header,
						shared_from_this(), asio::placeholders::error));
	}

	void handle_read_header(const boost::system::error_code& error) {
		DEBUG && (std::cerr << "handle read " << error.message() << std::endl);
		if (error) {
			return;
		}
		DEBUG && (std::cerr << "Got header!" << '\n' << show_hex(m_readbuf) << std::endl);
		unsigned start = 0;
		unsigned payload_length = m_framed_control.decode_header(m_readbuf, start);
		if (payload_length < 1) {
			start_read_header();
			return;
		}
		DEBUG && (std::cerr << payload_length << " bytes" << std::endl);
		start_read_body(start, payload_length);
	}

	/**
	 * called once a header has been successfully read.
	 * m_readbuf already contains the header in the first HEADER_SIZE bytes after start.
	 * Expand it to fit in the body as well, and start async read into the body.
	 * The final checksum is not included in the payload length.
	 */
	void start_read_body(const unsigned start, const unsigned payload_length) {
		
		m_readbuf.resize(start + HEADER_SIZE + payload_length + 4);
		asio::mutable_buffers_1 buf = 
			asio::buffer(&m_readbuf[start + HEADER_SIZE], payload_length + 4);
		asio::async_read(m_socket, buf,
				boost::bind(&BridgeConnection::handle_read_body,
						shared_from_this(), asio::placeholders::error));
	}

	void handle_read_body(const boost::system::error_code& error) {	
		if (error) {
			DEBUG && (std::cerr << "handle body " << error << std::endl);
			return;
		}
		DEBUG && (std::cerr << "Got body!" << '\n' << show_hex(m_readbuf) << std::endl);
		handle_request();
		start_read_header();
	}

	/**
	 * Called when enough data was read into m_readbuf for a complete request message.
	 * TODO: This is where the call to CREO is made.
	 */
	void handle_request() {
		if (!m_framed_control.unpack(m_readbuf)) {
			// log.warn("bad message, could not unpack");
			return;
		}
		ControlPointer req = m_framed_control.get_payload();
		m_edit_ref.push_front(*req);
	}

};

struct BridgeClient::BridgeClientImpl { 
	tcp::resolver m_resolver;
	edit_deque m_edit;
	std::string m_host;
	std::string m_service;

	BridgeClientImpl(asio::io_service& io_service, std::string host,
			std::string service) :  m_resolver(io_service), m_host(host), m_service(service) {
		start_resolve();
	}

	void start_resolve() {
		tcp::resolver::query query(m_host, m_service);
		m_resolver.async_resolve(query, 
			boost::bind(&BridgeClientImpl::handle_resolution, this, 
				asio::placeholders::error,  boost::asio::placeholders::iterator));
	}

	/**
	 * Called with connection iterator for each interface (should be just one).
	 */
	void handle_resolution(const boost::system::error_code &ec, tcp::resolver::iterator it) {
	  if (ec)  {
		  /** logger.warn("could not resolve m_host {} and port {}", m_host, port); */
		  return;
	  }
	  BridgeConnection::Pointer connection = BridgeConnection::create(m_resolver.get_io_service(), m_edit);
	  connection->start(it);
	}

};

BridgeClient::BridgeClient(boost::asio::io_service& io_service, const std::string host, const std::string service) :
		impl(new BridgeClientImpl(io_service, host, service)) 
{
}

BridgeClient::~BridgeClient() {
}

