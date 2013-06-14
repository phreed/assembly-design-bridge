/**
 * the-lift.cpp
 * Copyright (c) 2013 Fred Eisele (phreed at gmail dot com)
 *
 * Distributed under the Boost Software License, Version 1.0. (See accompanying
 * file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
 */

#include "BridgeClient.h"
#include "FramedMessage.h"
#include "CdbMsg.pb.h"

#include <cassert>
#include <iostream>
#include <map>
#include <deque>
#include <string>
#include <sstream>
#include <vector>
#include <boost/asio.hpp>
#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/cstdint.hpp>
#include <boost/enable_shared_from_this.hpp>

using namespace std;
namespace asio = boost::asio;
using asio::ip::tcp;
using boost::uint8_t;

#define DEBUG true

namespace meta = edu::vanderbilt::isis::meta;
typedef deque<meta::Control> EditDeque;

/**
 * connection - handles a connection with a single client.
 * Create only through the BridgeConnection::create factory.
 * <p>
 * As messages are received they are placed in a queue which
 * are then passed to the CREO api to update the model.
 */
class BridgeConnection: public boost::enable_shared_from_this<BridgeConnection> {
public:
	typedef boost::shared_ptr<BridgeConnection> Pointer;
	typedef boost::shared_ptr<meta::Control> ControlPointer;

	/**
	 * The factory function to create the bridge connection object.
	 */
	static Pointer create(asio::io_service& io_service, EditDeque& delta) {
		return Pointer(new BridgeConnection(io_service, delta));
	}

	tcp::socket& get_socket() {
		return m_socket;
	}

	void start(boost::asio::ip::tcp::resolver::iterator it) {
		m_socket.async_connect(*it, start_read_header);
		start_read_header();
	}

private:
	tcp::socket m_socket;
	EditDeque& m_delta_ref;
	vector<uint8_t> m_readbuf;
	FramedMessage<meta::Control> m_packed_request;

	BridgeConnection(asio::io_service& io_service, EditDeque& delta) :
			m_socket(io_service), m_delta_ref(delta), m_packed_request(
					boost::shared_ptr<meta::Control>(new meta::Control())) {
	}

	void start_read_header() {
		m_readbuf.resize(HEADER_SIZE);
		asio::async_read(m_socket, asio::buffer(m_readbuf),
				boost::bind(&BridgeConnection::handle_read_header,
						shared_from_this(), asio::placeholders::error));
	}

	void handle_read_header(const boost::system::error_code& error) {
		DEBUG && (cerr << "handle read " << error.message() << '\n');
		if (!error) {
			DEBUG && (cerr << "Got header!\n");
			DEBUG && (cerr << show_hex(m_readbuf) << endl);
			unsigned msg_len = m_packed_request.decode_header(m_readbuf);
			DEBUG && (cerr << msg_len << " bytes\n");
			start_read_body(msg_len);
		}
	}

	/**
	 * called once a header has been successfully read.
	 */
	void start_read_body(unsigned msg_len) {
		/*
		 * m_readbuf already contains the header in its first HEADER_SIZE bytes.
		 * Expand it to fit in the body as well, and start async read into the body.
		 */
		m_readbuf.resize(HEADER_SIZE + msg_len);
		asio::mutable_buffers_1 buf = asio::buffer(&m_readbuf[HEADER_SIZE],
				msg_len);
		asio::async_read(m_socket, buf,
				boost::bind(&BridgeConnection::handle_read_body,
						shared_from_this(), asio::placeholders::error));
	}

	void handle_read_body(const boost::system::error_code& error) {
		DEBUG && (cerr << "handle body " << error << '\n');
		if (!error) {
			DEBUG && (cerr << "Got body!\n");
			DEBUG && (cerr << show_hex(m_readbuf) << endl);
			handle_request();
			start_read_header();
		}
	}

	/**
	 * Called when enough data was read into m_readbuf for a complete request message.
	 * TODO: This is where the call to CREO is made.
	 */
	void handle_request() {
		if (!m_packed_request.unpack(m_readbuf)) {
			// log.warn("bad message, could not unpack");
			return;
		}
		ControlPointer req = m_packed_request.get_msg();
		m_delta_ref.push_front(*req);
	}

};

struct BridgeClient::BridgeClientImpl {
	tcp::resolver m_resolver;
	EditDeque m_delta;
	std::string m_host;
	unsigned m_port;

	BridgeClientImpl(asio::io_service& io_service, std::string host,
			unsigned port) :  m_resolver(io_service), m_host(host), m_port(port) {
		start_resolve();
	}

	void start_resolve() {
		tcp::resolver::query query(m_host, m_port);
		m_resolver.async_resolve(query, handle_resolve);
	}

	/**
	 * Called with connection iterator for each interface (should be just one).
	 */
	void handle_resolve(const boost::system::error_code &ec, tcp::resolver::iterator it)
	{
	  if (ec)  {
		  /** logger.warn("could not resolve m_host {} and port {}", m_host, port); */
		  return;
	  }
	  BridgeConnection::Pointer connection = BridgeConnection::create(m_resolver.get_io_service(), m_delta);
	  connection->start(it);
	}

};

BridgeClient::BridgeClient(asio::io_service& io_service, std::string host, unsigned port) :
		impl(new BridgeClientImpl(io_service, host, port)) {
}

BridgeClient::~BridgeClient() {
}

