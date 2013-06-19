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

#include "gen/MetaLinkMsg.pb.h"
#include "BridgeClient.h"

/**
 * Read lines from standard input and write them to the socket on the main thread.
 * Create a secondary thread (task) to receive messages from the server.
 *
 */
int main(int argc, char* argv[]) {
	namespace po = boost::program_options;

	try {
		// Declare the supported options.
		po::options_description odesc("Allowed options");
		std::string host;
		std::string port;
		// @formatter off
		odesc.add_options()
				("help", "produce help message")
				("host", po::value<std::string>(&host)->default_value("localhost"), "set host name (or ip addr)")
				("port", po::value<std::string>(&port)->default_value("15150"), "set port number or service name")
				("initfile", po::value<std::string>(), "initial load")
				;
		// @formater on

		po::variables_map varmap;
		po::parsed_options parsedOptions = po::parse_command_line(argc, argv, odesc);
		po::store(parsedOptions, varmap);
		po::notify(varmap);

		if (varmap.count("help")) {
			std::cout << odesc << std::endl;
			return 1;
		}

		std::istream *instream;
		if (varmap.count("initfile")) {
			std::string file = varmap["file"].as<std::string>();
			std::cout << "file name was set to " << varmap["initfile"].as<std::string>()
					<< "." << std::endl;
			std::ifstream ifs;
			ifs.open(file.c_str(), std::ios::in | std::ios::binary);
			instream = &ifs;
		} else {
			std::cout << "standard input being used" << std::endl;
			instream = &std::cin;
		}

		boost::asio::io_service io_service;
		BridgeClient client(io_service, host, port);
		io_service.run();

	} catch (std::exception& ex) {
		std::cerr << "Exception: " << ex.what() << "\n";
	}

	return 0;
}
