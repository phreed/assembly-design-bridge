/**
 * the-lift.cpp
 * Copyright (c) 2013 Fred Eisele (phreed at gmail dot com)
 *
 * Distributed under the Boost Software License, Version 1.0. (See accompanying
 * file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
 */

#include "BridgeClient.h"
#include "FramedEdit.h"
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
#include <google/protobuf/text_format.h>
// #include <boost/log/trivial.hpp>

namespace asio = boost::asio;
using asio::ip::tcp;
using boost::uint8_t;

#define DEBUG true

namespace meta = edu::vanderbilt::isis::meta;
namespace pb = google::protobuf;

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
    typedef boost::shared_ptr<meta::Control> EditPointer;
    typedef std::deque<EditPointer> EditDeque;

	typedef boost::shared_ptr<meta::Payload> PayloadtPointer;
	

	/**
	 * The factory function to create the bridge connection object.
	 */
	static Pointer create(asio::io_service& io_service, EditDeque& edit) {
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
	EditDeque& m_edit_ref;
	FramedEdit<meta::Control> m_framed_control;

	BridgeConnection(asio::io_service& io_service, EditDeque& edit) :
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
		m_framed_control.resize_input_buffer_for_header();
		asio::async_read(m_socket, m_framed_control.get_input_buffer_for_header(),
				boost::bind(&BridgeConnection::handle_read_header,
						shared_from_this(), asio::placeholders::error));
	}

	void handle_read_header(const boost::system::error_code& error) {
		DEBUG && (std::cerr << "handle read " << error.message() << std::endl);
		if (error) {
			return;
		}
		DEBUG && (std::cerr << "Got header!" << '\n' << m_framed_control.show_input_buffer() << std::endl);
		unsigned payload_length = m_framed_control.decode_header();
		if (payload_length < 1) {
			start_read_header();
			return;
		}
		DEBUG && (std::cerr << payload_length << " bytes" << std::endl);
		start_read_body();
	}

	/**
	 * called once a header has been successfully read.
	 * m_readbuf already contains the header in the first HEADER_SIZE bytes after start.
	 * Expand it to fit in the body as well, and start async read into the body.
	 * The final checksum is not included in the payload length.
	 */
	void start_read_body() {
		m_framed_control.resize_input_buffer_for_load();
		
		asio::async_read(m_socket, m_framed_control.get_input_buffer_for_payload(),
				boost::bind(&BridgeConnection::handle_read_body,
						shared_from_this(), asio::placeholders::error));
	}

	void handle_read_body(const boost::system::error_code& error) {	
		if (error) {
			DEBUG && (std::cerr << "handle body " << error << std::endl);
			return;
		}
		DEBUG && (std::cerr << "Got body!" << '\n' << m_framed_control.show_input_buffer() << std::endl);
		handle_request();
		start_read_header();
	}

	/**
	 * Called when enough data was read into m_readbuf for a complete request message.
	 * TODO: This is where the call to CREO is made.
	 */
	void handle_request() {
		bool success = m_framed_control.unpack();
		if (!success) {
			DEBUG && (std::cerr << "handle request could not unpack " << std::endl);
			// log.warn("bad message, could not unpack");
			return;
		}
		EditPointer edit = m_framed_control.get_load();
		std::cout << "edit " << edit << std::endl;
		m_edit_ref.push_front(edit);

		pb::RepeatedPtrField< meta::PayloadRaw > prl = edit->payload();
		for (int ix=0; ix < prl.size(); ++ix) {
			meta::PayloadRaw pr = prl.Get(ix);
			const std::string payloadBytes = pr.payload();
			switch (pr.encoding()) {
			case meta::PayloadRaw_EncodingType_PROTOBUF: 
				{
				meta::Payload pay;
				pay.ParseFromString(payloadBytes);
				std::string display;
				pb::TextFormat::PrintToString(pay, &display);
				std::cout << "payload [" << ix << "]\n" << display << std::endl;
				break;														   
				}
			case  meta::PayloadRaw_EncodingType_XML: 
				{
					DEBUG && (std::cout << "xml string supplied \n" << payloadBytes << std::endl);
					break;
				}
			default:
				{}
			}
		}
	}

};

struct BridgeClient::BridgeClientImpl { 
	tcp::resolver m_resolver;
	BridgeConnection::EditDeque m_edit;
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

