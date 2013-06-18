#ifndef FRAMED_PAYLOAD_H
#define FRAMED_PAYLOAD_H

#include <string>
#include <cassert>
#include <vector>
#include <cstdio>
#include <boost/shared_ptr.hpp>
#include <boost/cstdint.hpp>
#include <boost/crc.hpp> 

using boost::uint8_t;
using boost::uint32_t;

class DataBuf {
public:
	DataBuf(const std::vector<uint8_t> buf) {
		m_buf = buf;
		m_start = 0;
		m_payload_length = 0;
	}
	std::vector<uint8_t> m_buf;
	unsigned m_start;
	unsigned m_payload_length;
};

typedef std::vector<uint8_t> data_buffer;

// A generic function to show contents of a container holding byte data
// as a string with hex representation for each byte.
//
template <class CharContainer>
std::string show_hex(const CharContainer& container) {
	std::ostringstream formatter; 
    typename CharContainer::const_iterator ci;
    for (ci = container.begin(); ci != container.end(); ++ci) {
		formatter << std::hex << static_cast<int>(*ci);
    }
    return formatter.str();
}


/**
 * The header size for framed messages
 */
const unsigned HEADER_SIZE = 20;


/**
 * A FamedMessage implements simple "packing" of protocol buffers Messages into
 * a string prepended by a header specifying at least the payload length.
 * MessageType should be a Message class generated by the protobuf compiler.
 */
template <class MessageType>
class FramedPayload
{
public:
    typedef boost::shared_ptr<MessageType> PayloadPointer;

    FramedPayload(PayloadPointer msg = PayloadPointer())
        : m_payload(msg)
    {}

    void set_payload(PayloadPointer payload)
    {
        m_payload = payload;
    }

    PayloadPointer get_payload()
    {
        return m_payload;
    }

    /**
     *
     * Pack the message into the given data_buffer.
     * The buffer is resized to exactly fit the message.
     * Return false in case of an error, true if successful.
*/
    bool pack(data_buffer& buf) const
    {
        if (!m_payload)
            return false;

        unsigned msg_size = m_payload->ByteSize();
        buf.resize(HEADER_SIZE + msg_size);
        encode_header(buf, msg_size);
        return m_payload->SerializeToArray(&buf[HEADER_SIZE], msg_size);
    }

    /**
     * Given a buffer with the first HEADER_SIZE bytes representing the header,
     * decode the header and return the message length.
	 * <p>
	 * <table>
	 * <tr><th>size (bytes)</th><th>encoding</th><th>purpose</th></tr>
	 * <tr><td>4</td><td>0xdeadbeef</td><td>magic indicates start of frame </td></tr>
	 * <tr><td>4</td><td>big endian 32 bit integer, bytes</td><td>size of the payload</td></tr>
	 * <tr><td>1</td><td>error code</td><td>error</td></tr>
	 * <tr><td>1</td><td>8 bit integer, higher is greater</td><td>priority</td></tr>
	 * <tr><td>2</td><td>bits</td><td>reserved</td></tr>
	 * <tr><td>4</td><td>crc32 checksum</td><td>payload validation</td></tr>
	 * <tr><td>4</td><td>crc32 checksum</td><td>header validation (previous 16 bytes)</td></tr>
	 * <tr><td>(size of the payload)</td><td>protocol buffer bytes</td><td>payload</td></tr>
	 * <tr><td>4</td><td>crc32 checksum</td><td>payload validation (repeated)</td></tr>
	 * </table>
	 *
	 * Rough procedure...
	 * <ol>
	 * <li>Check to make sure there are at least the minimum nuber of bytes</li>
	 * <li>Look for the magic</li>
	 * <li>Extract the payload length</li>
	 * </ol>
     * Return 0 in case of an error.
     */
    unsigned decode_header(const data_buffer& buf, unsigned& start) const {
		unsigned last_relevant_position = buf.size() - sizeof(magic) + 1;
        for (unsigned ix = 0; ix < last_relevant_position; ++ix) {
			if (buf[ix] != magic[0]) {
				continue;
			}
			if (buf.size() < start + HEADER_SIZE) {
				// logger.debug("not enough bytes to contain a header {}", buf.size());
				return 0;
			}
			start = ix;
  			++ix;
			if (buf[ix] != magic[1]) {
					continue;
			}
			++ix;
			if (buf[ix] != magic[2]) {	
				continue;
			}
			++ix;
			if (buf[ix] != magic[3]) {	
				continue;
			}
			++ix;
			unsigned jx = ix;
			/* read the payload length in network order */
		    uint32_t payload_length = unpackUint32(buf, jx);
			
			/* scik some stuff */
			jx += 1 + 1 + 2;

			uint32_t payload_checksum = unpackChecksum(buf, jx);
			uint32_t header_checksum = unpackChecksum(buf, jx);
			/* TODO validate the header against its checksum */

			return payload_length;
        }
        return 0;
    }

    /**
     * Unpack and store a message from the given packed buffer.
     * Return true if unpacking successful, false otherwise.
     */
    bool unpack(const data_buffer& buf)
    {
        return m_payload->ParseFromArray(&buf[HEADER_SIZE], buf.size() - HEADER_SIZE);
    }

private:
	 static uint8_t const magic[4];

	/**
	 * unpack an integer in network order 
	 */
	uint32_t unpackUint32(const data_buffer& buf,  unsigned& ix) const {
			unsigned length = 0;
			for (unsigned hx=0; hx < sizeof(length); ++hx, ++ix) {
                  length = length * 256 + (static_cast<unsigned>(buf[ix]) & 0x0FF);
			}
			return length;
	}
	/**
	 * unpack a checksum
	 */
	uint32_t unpackChecksum(const data_buffer& buf,  unsigned& ix) const {
		boost::crc_32_type result;
		for (unsigned hx=0; hx < sizeof(uint32_t); ++hx, ++ix) {
			result.process_byte(buf[hx]);
		}
		return result.checksum();
	}

    /**
     * Encodes the side into a header at the beginning of buf
    */
    void encode_header(data_buffer& buf, unsigned size) const
    {
        assert(buf.size() >= HEADER_SIZE);
        buf[0] = static_cast<uint8_t>((size >> 24) & 0xFF);
        buf[1] = static_cast<uint8_t>((size >> 16) & 0xFF);
        buf[2] = static_cast<uint8_t>((size >> 8) & 0xFF);
        buf[3] = static_cast<uint8_t>(size & 0xFF);
    }

    PayloadPointer m_payload;
};

#endif /* FRAMED_PAYLOAD_H */