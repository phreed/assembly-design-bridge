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
		// @formatter off
		desc.add_options()
				("help", "produce help message")
				("host",po::value<std::string>(), "set host name (or ip addr)")
				("port",po::value<int>(), "set port number")
				("initfile",po::value<std::string>(), "initial load")
				;
		// @formater on

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
		if (vm.count("initfile")) {
			std::string file = vm["file"];
			std::cout << "file name was set to " << vm["file"].as<std::string>()
					<< "." << std::endl;
			std::ifstream ifs;
			ifs.open(file.c_str(), std::ios::in | std::ios::binary);
		} else {
			std::cout << "standard input being used" << std::endl;
			instream = std::cin;
		}

		boost::asio::io_service io_service;
		BridgeClient client(io_service, host, port);
		io_service.run();

	} catch (std::exception& ex) {
		std::cerr << "Exception: " << ex.what() << "\n";
	}

	return 0;
}
