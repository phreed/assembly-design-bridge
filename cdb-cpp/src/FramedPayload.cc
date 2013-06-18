

#include "BridgeClient.h"
#include "FramedPayload.h"
#include "CdbMsg.pb.h"

namespace meta = edu::vanderbilt::isis::meta;

const uint8_t FramedPayload<meta::Control>::magic[4] = { 0xDE, 0xAD, 0xBE, 0xEF };