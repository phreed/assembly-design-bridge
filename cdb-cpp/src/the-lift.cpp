/**
 * the-lift.cpp
 * Copyright (c) 2013 Fred Eisele (phreed at gmail dot com)
 *
 * Distributed under the Boost Software License, Version 1.0. (See accompanying
 * file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
 */

#include <cstdlib>
#include <deque>
#include <iostream>
#include <fstream>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/program_options.hpp>
#include "CdbMsg.pb.h"

using boost::asio::ip::tcp;
using edu::vanderbilt::isis::meta;

typedef std::deque<meta::frame> the_message_queue;

/**
 * This client connects to "The-Bridge" which is a server of
 * "meta::frame" messages serialized to a stream.
 */
class the_lift {
public:
	the_lift(boost::asio::io_service& io_service,
			tcp::resolver::iterator endpoint_iterator) :
			io_service_(io_service), socket_(io_service) {
		tcp::endpoint endpoint = *endpoint_iterator;
		socket_.async_connect(endpoint,
				boost::bind(&the_lift::handle_connect, this,
						boost::asio::placeholders::error, ++endpoint_iterator));
	}

	void write(const meta::frame& msg) {
		io_service_.post(boost::bind(&the_lift::do_write, this, msg));
	}

	void close() {
		io_service_.post(boost::bind(&the_lift::do_close, this));
	}

private:

	/**
	 * Connect to the server and set up the call back for reading the frame.
	 */
	void handle_connect(const boost::system::error_code& error,
			tcp::resolver::iterator endpoint_iterator) {
		if (!error) {
			if (endpoint_iterator != tcp::resolver::iterator()) {
				socket_.close();
				tcp::endpoint endpoint = *endpoint_iterator;
				socket_.async_connect(endpoint,
						boost::bind(&the_lift::handle_connect, this,
								boost::asio::placeholders::error,
								++endpoint_iterator));
			}
			return;
		}
		boost::asio::async_read(socket_,
				boost::asio::buffer(read_msg_.data(),
						meta::frame::header_length),
				boost::bind(&the_lift::handle_read_frame_header, this,
						boost::asio::placeholders::error));

	}

	/**
	 * Read the frame header.
	 */
	void handle_read_frame_header(const boost::system::error_code& error) {
		if (error) {
			do_close();
			return;
		}
		if (!read_msg_.decode_header()) {
			do_close();
			return;
		}
		boost::asio::async_read(socket_,
				boost::asio::buffer(read_msg_.body(), read_msg_.body_length()),
				boost::bind(&the_lift::handle_read_body, this,
						boost::asio::placeholders::error));

	}

	void handle_read_body(const boost::system::error_code& error) {
		if (error) {
			do_close();
			return;
		}
		std::cout.write(read_msg_.body(), read_msg_.body_length());
		std::cout << "\n";
		boost::asio::async_read(socket_,
				boost::asio::buffer(read_msg_.data(),
						meta::frame::header_length),
				boost::bind(&the_lift::handle_read_frame_header, this,
						boost::asio::placeholders::error));

	}

	void do_write(meta::frame msg) {
		bool write_in_progress = !write_msgs_.empty();
		write_msgs_.push_back(msg);
		if (!write_in_progress) {
			boost::asio::async_write(socket_,
					boost::asio::buffer(write_msgs_.front().data(),
							write_msgs_.front().length()),
					boost::bind(&the_lift::handle_write, this,
							boost::asio::placeholders::error));
		}
	}

	void handle_write(const boost::system::error_code& error) {
		if (error) {
			do_close();
			return;
		}
		write_msgs_.pop_front();
		if (write_msgs_.empty()) {
			return;
		}
		boost::asio::async_write(socket_,
				boost::asio::buffer(write_msgs_.front().data(),
						write_msgs_.front().length()),
				boost::bind(&the_lift::handle_write, this,
						boost::asio::placeholders::error));
	}

	void do_close() {
		socket_.close();
	}

private:
	boost::asio::io_service& io_service_;
	tcp::socket socket_;
	meta::frame read_msg_;
	the_message_queue write_msgs_;
};

/**
 * Read lines from standard input and write them to the socket on the main thread.
 * Create a secondary thread (task) to receive messages from the server.
 *
 */
int main(int argc, char* argv[]) {
	namespace po = boost::program_options;

	try {
		// Declare the supported options.
		po::options_description desc("Allowed options");
		desc.add_options()("help", "produce help message")("host",
				po::value<std::string>(), "set host name (or ip addr)")("port",
				po::value<int>(), "set port number")("file",
				po::value<std::string>(), "input stream");

		po::variables_map vm;
		po::store(po::parse_command_line(argc, argv, desc), vm);
		po::notify(vm);

		if (vm.count("help")) {
			std::cout << desc << std::endl;
			return 1;
		}

		std::string host;
		if (vm.count("host")) {
			host = vm["host"];
			std::cout << "host name was set to " << host << "." << std::endl;
		} else {
			host = "localhost";
			std::cout << "host name was not set, default used." << std::endl;
		}

		int port;
		if (vm.count("port")) {
			port = vm["port"];
			std::cout << "port number was set to "
					<< vm["port"].as<std::string>() << "." << std::endl;
		} else {
			host = "15150";
			std::cout << "port number was not set, default used." << std::endl;
		}

		std::istream instream;
		if (vm.count("file")) {
			std::string file = vm["file"];
			std::cout << "file name was set to " << vm["file"].as<std::string>()
					<< "." << std::endl;
			std::ifstream f;
			f.open(file);
		} else {
			std::cout << "standard input being used" << std::endl;
			instream = std::cin;
		}

		boost::asio::io_service io_service;

		tcp::resolver resolver(io_service);
		tcp::resolver::query query(host, port);
		tcp::resolver::iterator iterator = resolver.resolve(query);

		the_lift client(io_service, iterator);

		std::size_t (boost::asio::io_service::*run_noargs)() = &boost::asio::io_service::run;
		boost::thread task(boost::bind(run_noargs, &io_service));

		char line[meta::frame::max_body_length + 1];
		while (std::cin.getline(line, meta::frame::max_body_length + 1)) {
			meta::frame msg;
			msg.body_length(std::strlen(line));
			std::memcpy(msg.body(), line, msg.body_length());
			msg.encode_header();
			client.write(msg);
		}

		client.close();
		task.join();
	} catch (std::exception& ex) {
		std::cerr << "Exception: " << ex.what() << "\n";
	}

	return 0;
}
