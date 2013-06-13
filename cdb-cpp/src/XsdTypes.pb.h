// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: XsdTypes.proto

#ifndef PROTOBUF_XsdTypes_2eproto__INCLUDED
#define PROTOBUF_XsdTypes_2eproto__INCLUDED

#include <string>

#include <google/protobuf/stubs/common.h>

#if GOOGLE_PROTOBUF_VERSION < 2005000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers.  Please update
#error your headers.
#endif
#if 2005000 < GOOGLE_PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers.  Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/message.h>
#include <google/protobuf/repeated_field.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/unknown_field_set.h>
// @@protoc_insertion_point(includes)

namespace edu {
namespace vanderbilt {
namespace isis {
namespace meta {

// Internal implementation detail -- do not call these.
void  protobuf_AddDesc_XsdTypes_2eproto();
void protobuf_AssignDesc_XsdTypes_2eproto();
void protobuf_ShutdownFile_XsdTypes_2eproto();

class ID;

// ===================================================================

class ID : public ::google::protobuf::Message {
 public:
  ID();
  virtual ~ID();

  ID(const ID& from);

  inline ID& operator=(const ID& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::google::protobuf::UnknownFieldSet& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::google::protobuf::UnknownFieldSet* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const ::google::protobuf::Descriptor* descriptor();
  static const ID& default_instance();

  void Swap(ID* other);

  // implements Message ----------------------------------------------

  ID* New() const;
  void CopyFrom(const ::google::protobuf::Message& from);
  void MergeFrom(const ::google::protobuf::Message& from);
  void CopyFrom(const ID& from);
  void MergeFrom(const ID& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  ::google::protobuf::uint8* SerializeWithCachedSizesToArray(::google::protobuf::uint8* output) const;
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:

  ::google::protobuf::Metadata GetMetadata() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required bytes uuid = 1;
  inline bool has_uuid() const;
  inline void clear_uuid();
  static const int kUuidFieldNumber = 1;
  inline const ::std::string& uuid() const;
  inline void set_uuid(const ::std::string& value);
  inline void set_uuid(const char* value);
  inline void set_uuid(const void* value, size_t size);
  inline ::std::string* mutable_uuid();
  inline ::std::string* release_uuid();
  inline void set_allocated_uuid(::std::string* uuid);

  // @@protoc_insertion_point(class_scope:edu.vanderbilt.isis.meta.ID)
 private:
  inline void set_has_uuid();
  inline void clear_has_uuid();

  ::google::protobuf::UnknownFieldSet _unknown_fields_;

  ::std::string* uuid_;

  mutable int _cached_size_;
  ::google::protobuf::uint32 _has_bits_[(1 + 31) / 32];

  friend void  protobuf_AddDesc_XsdTypes_2eproto();
  friend void protobuf_AssignDesc_XsdTypes_2eproto();
  friend void protobuf_ShutdownFile_XsdTypes_2eproto();

  void InitAsDefaultInstance();
  static ID* default_instance_;
};
// ===================================================================


// ===================================================================

// ID

// required bytes uuid = 1;
inline bool ID::has_uuid() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void ID::set_has_uuid() {
  _has_bits_[0] |= 0x00000001u;
}
inline void ID::clear_has_uuid() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void ID::clear_uuid() {
  if (uuid_ != &::google::protobuf::internal::kEmptyString) {
    uuid_->clear();
  }
  clear_has_uuid();
}
inline const ::std::string& ID::uuid() const {
  return *uuid_;
}
inline void ID::set_uuid(const ::std::string& value) {
  set_has_uuid();
  if (uuid_ == &::google::protobuf::internal::kEmptyString) {
    uuid_ = new ::std::string;
  }
  uuid_->assign(value);
}
inline void ID::set_uuid(const char* value) {
  set_has_uuid();
  if (uuid_ == &::google::protobuf::internal::kEmptyString) {
    uuid_ = new ::std::string;
  }
  uuid_->assign(value);
}
inline void ID::set_uuid(const void* value, size_t size) {
  set_has_uuid();
  if (uuid_ == &::google::protobuf::internal::kEmptyString) {
    uuid_ = new ::std::string;
  }
  uuid_->assign(reinterpret_cast<const char*>(value), size);
}
inline ::std::string* ID::mutable_uuid() {
  set_has_uuid();
  if (uuid_ == &::google::protobuf::internal::kEmptyString) {
    uuid_ = new ::std::string;
  }
  return uuid_;
}
inline ::std::string* ID::release_uuid() {
  clear_has_uuid();
  if (uuid_ == &::google::protobuf::internal::kEmptyString) {
    return NULL;
  } else {
    ::std::string* temp = uuid_;
    uuid_ = const_cast< ::std::string*>(&::google::protobuf::internal::kEmptyString);
    return temp;
  }
}
inline void ID::set_allocated_uuid(::std::string* uuid) {
  if (uuid_ != &::google::protobuf::internal::kEmptyString) {
    delete uuid_;
  }
  if (uuid) {
    set_has_uuid();
    uuid_ = uuid;
  } else {
    clear_has_uuid();
    uuid_ = const_cast< ::std::string*>(&::google::protobuf::internal::kEmptyString);
  }
}


// @@protoc_insertion_point(namespace_scope)

}  // namespace meta
}  // namespace isis
}  // namespace vanderbilt
}  // namespace edu

#ifndef SWIG
namespace google {
namespace protobuf {


}  // namespace google
}  // namespace protobuf
#endif  // SWIG

// @@protoc_insertion_point(global_scope)

#endif  // PROTOBUF_XsdTypes_2eproto__INCLUDED