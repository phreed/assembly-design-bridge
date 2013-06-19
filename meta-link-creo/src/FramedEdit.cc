

#include "BridgeClient.h"
#include "FramedEdit.h"
#include "gen/MetaLinkMsg.pb.h"

namespace meta = edu::vanderbilt::isis::meta;

const uint8_t FramedEdit<meta::Edit>::magic[4] = { 0xDE, 0xAD, 0xBE, 0xEF };