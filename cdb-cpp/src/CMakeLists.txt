
project (THE_LIFT)

set (THE_LIFT_SRCS the-lift.cc BridgeClient.cc CdbMsg.pb.cc AssemblyInterface.pb.cc XsdTypes.pb.cc)

add_executable (the-lift ${THE_LIFT_SRCS})

