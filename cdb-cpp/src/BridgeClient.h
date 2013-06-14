

#ifndef BRIDGE_CLIENT_H
#define BRIDGE_CLIENT_H

/**
 * Bridge server connection.
 *
 */
class BridgeClient {
public:
	BridgeClient(boost::asio::io_service&, std::string host, unsigned port);
	~BridgeClient();

private:
	BridgeClient();
	void start_connect();

	struct BridgeClientImpl;
	boost::scoped_ptr<BridgeClientImpl> impl;

};
#endif // BRIDGE_CLIENT_H
