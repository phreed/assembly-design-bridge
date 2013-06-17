

#ifndef BRIDGE_CLIENT_H
#define BRIDGE_CLIENT_H

#include <boost/asio.hpp>
#include <boost/bind.hpp>
#include <boost/smart_ptr.hpp>

/**
 * Bridge server connection.
 *
 */
class BridgeClient {
public:
	BridgeClient(boost::asio::io_service& io_service, const std::string host, const std::string service);
	~BridgeClient();

private:
	void start_connect();

	struct BridgeClientImpl;
	boost::scoped_ptr<BridgeClientImpl> impl;

};
#endif // BRIDGE_CLIENT_H
