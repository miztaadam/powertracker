import {
  protoOf180f3jzyo7rfj as protoOf,
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  VOID3gxj6tk5isa35 as VOID,
  getKClassFromExpression348iqjl4fnx2f as getKClassFromExpression,
  asList2ho2pewtsfvv as asList,
  objectCreate1ve4bgxiu4x98 as objectCreate,
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  StringCompanionObject_instance2dx16b5pp3v3z as StringCompanionObject_instance,
  constructCallableReference23y65rf941mch as constructCallableReference,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  emptyList1g2z5xcrvp2zy as emptyList,
  LazyThreadSafetyMode_PUBLICATION_getInstance23f213579at67 as LazyThreadSafetyMode_PUBLICATION_getInstance,
  lazy1261dae0bgscp as lazy,
  IllegalArgumentException_init_$Create$1gox8sdqs91jy as IllegalArgumentException_init_$Create$,
  zip2suipyqmdw72q as zip,
  toMap1vec9topfei08 as toMap,
  LinkedHashMap_init_$Create$s27yccuovnls as LinkedHashMap_init_$Create$,
  toString1pkumu07cwy4m as toString,
  IllegalStateException_init_$Create$38ax6c90susjc as IllegalStateException_init_$Create$,
  mapCapacity1h45rc3eh9p2l as mapCapacity,
  LinkedHashMap_init_$Create$21wpz6bm2n4yn as LinkedHashMap_init_$Create$_0,
  KProperty1ca4yb4wlo496 as KProperty1,
  getPropertyCallableRef3hckxc0xueiaj as getPropertyCallableRef,
  THROW_CCE2g6jy02ryeudk as THROW_CCE,
  KtMap140uvy3s5zad8 as KtMap,
  isInterface3d6p8outrmvmk as isInterface,
  captureStack1fzi4aczwc4hg as captureStack,
  IllegalArgumentException_init_$Init$2rzql1b7hphui as IllegalArgumentException_init_$Init$,
  IllegalArgumentException_init_$Init$c3q2g5ghz7m0 as IllegalArgumentException_init_$Init$_0,
  IllegalArgumentException_init_$Init$1kkn5cqzzyumg as IllegalArgumentException_init_$Init$_1,
  IllegalArgumentException2asla15b5jaob as IllegalArgumentException,
  ArrayList_init_$Create$2xrifdqz5xirg as ArrayList_init_$Create$,
  HashSet_init_$Create$b2fvq2b4c4v2 as HashSet_init_$Create$,
  isBlank1dvkhjjvox3p0 as isBlank,
  equals2au1ep9vhcato as equals,
  toList383f556t1dixk as toList,
  toHashSet1qrcsl3g8ugc8 as toHashSet,
  copyToArray2j022khrow2yi as copyToArray,
  toBooleanArray2u3qw7fjwsmuh as toBooleanArray,
  withIndex3s8q7w1g0hyfn as withIndex,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  ArrayList_init_$Create$a978fvbbts3d as ArrayList_init_$Create$_0,
  to2cs3ny02qtbcb as to,
  lazy2hsh8ze7j6ikd as lazy_0,
  contentEqualsaf55p28mnw74 as contentEquals,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  ensureNotNull1e947j3ixpazm as ensureNotNull,
  getStringHashCode26igk1bx568vk as getStringHashCode,
  Long2qws0ah9gnpki as Long,
  toIntOrNull3w2d066r9pvwm as toIntOrNull,
  hashCodeq5arwsb9dgti as hashCode,
  IndexOutOfBoundsException_init_$Create$k95f9vzk11af as IndexOutOfBoundsException_init_$Create$,
  ArrayList3it5z8td81qkl as ArrayList,
  ArrayList_init_$Create$2rx6gww4owbo3 as ArrayList_init_$Create$_1,
  KtList3hktaavzmj137 as KtList,
  Collection1k04j3hzsbod0 as Collection,
  coerceAtLeast2bkz8m9ik7hep as coerceAtLeast,
  longArray288a0fctlmjmj as longArray,
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  get_lastIndex1y2f6o9u8hnf7 as get_lastIndex,
  shiftLeft1ck77p6vapyra as shiftLeft,
  bitwiseOr1ita6dahwp8zb as bitwiseOr,
  equalsLong28bsrfhwvd686 as equalsLong,
  invert3i8k5n0dd6oib as invert,
  countTrailingZeroBits1k55x07cygoff as countTrailingZeroBits,
  indexOf3ic8eacwbbrog as indexOf,
  contentToString3ujacv8hqfipd as contentToString,
  Enum3alwj03lh1n41 as Enum,
  joinToString1cxrrlmo0chqs as joinToString,
  HashSet_init_$Create$2izef1i1668r1 as HashSet_init_$Create$_0,
  contentHashCode2i020q5tbeh2s as contentHashCode,
  until1jbpn0z3f8lbg as until,
  HashMap_init_$Create$g6b5bm370ovq as HashMap_init_$Create$,
  booleanArray2jdug9b51huk7 as booleanArray,
  emptyMapr06gerzljqtm as emptyMap,
  charSequenceLength3278n89t01tmv as charSequenceLength,
  lastOrNull1aq5oz189qoe1 as lastOrNull,
  get_lastIndex1yw0x4k50k51w as get_lastIndex_0,
  get_indicesc04v40g017hw as get_indices,
  get_indices377latqcai313 as get_indices_0,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
initMetadataForInterface(SerializationStrategy, 'SerializationStrategy');
initMetadataForInterface(DeserializationStrategy, 'DeserializationStrategy');
initMetadataForInterface(KSerializer, 'KSerializer', VOID, VOID, [SerializationStrategy, DeserializationStrategy]);
initMetadataForClass(SealedClassSerializer$$inlined$groupingBy$1);
initMetadataForClass(AbstractPolymorphicSerializer, 'AbstractPolymorphicSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(SealedClassSerializer, 'SealedClassSerializer', VOID, AbstractPolymorphicSerializer);
initMetadataForClass(SerializationException, 'SerializationException', SerializationException_init_$Create$, IllegalArgumentException);
initMetadataForClass(MissingFieldException, 'MissingFieldException', VOID, SerializationException);
initMetadataForClass(UnknownFieldException, 'UnknownFieldException', VOID, SerializationException);
function get_isNullable() {
  return false;
}
function get_isInline() {
  return false;
}
function get_annotations() {
  return emptyList();
}
initMetadataForInterface(SerialDescriptor, 'SerialDescriptor');
initMetadataForClass(ContextDescriptor, 'ContextDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForClass(elementDescriptors$1);
initMetadataForClass(elementDescriptors$$inlined$Iterable$1);
initMetadataForClass(elementNames$1);
initMetadataForClass(elementNames$$inlined$Iterable$1);
initMetadataForClass(ClassSerialDescriptorBuilder, 'ClassSerialDescriptorBuilder');
initMetadataForInterface(CachedNames, 'CachedNames');
initMetadataForClass(SerialDescriptorImpl, 'SerialDescriptorImpl', VOID, VOID, [SerialDescriptor, CachedNames]);
initMetadataForClass(SerialKind, 'SerialKind');
initMetadataForClass(PrimitiveKind, 'PrimitiveKind', VOID, SerialKind);
initMetadataForObject(INT, 'INT', VOID, PrimitiveKind);
initMetadataForObject(LONG, 'LONG', VOID, PrimitiveKind);
initMetadataForObject(DOUBLE, 'DOUBLE', VOID, PrimitiveKind);
initMetadataForObject(STRING, 'STRING', VOID, PrimitiveKind);
initMetadataForClass(PolymorphicKind, 'PolymorphicKind', VOID, SerialKind);
initMetadataForObject(SEALED, 'SEALED', VOID, PolymorphicKind);
initMetadataForObject(ENUM, 'ENUM', VOID, SerialKind);
initMetadataForObject(CONTEXTUAL, 'CONTEXTUAL', VOID, SerialKind);
initMetadataForClass(StructureKind, 'StructureKind', VOID, SerialKind);
initMetadataForObject(CLASS, 'CLASS', VOID, StructureKind);
initMetadataForObject(LIST, 'LIST', VOID, StructureKind);
initMetadataForObject(MAP, 'MAP', VOID, StructureKind);
initMetadataForObject(OBJECT, 'OBJECT', VOID, StructureKind);
function decodeSerializableValue(deserializer) {
  return deserializer.yc(this);
}
initMetadataForInterface(Decoder, 'Decoder');
function decodeSequentially() {
  return false;
}
function decodeCollectionSize(descriptor) {
  return -1;
}
function decodeSerializableElement$default(descriptor, index, deserializer, previousValue, $super) {
  previousValue = previousValue === VOID ? null : previousValue;
  return $super === VOID ? this.xf(descriptor, index, deserializer, previousValue) : $super.xf.call(this, descriptor, index, deserializer, previousValue);
}
initMetadataForInterface(CompositeDecoder, 'CompositeDecoder');
initMetadataForClass(AbstractDecoder, 'AbstractDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
function encodeNotNullMark() {
}
function beginCollection(descriptor, collectionSize) {
  return this.rf(descriptor);
}
function encodeSerializableValue(serializer, value) {
  serializer.zc(this, value);
}
function encodeNullableSerializableValue(serializer, value) {
  var isNullabilitySupported = serializer.xc().yd();
  if (isNullabilitySupported) {
    return this.rg(isInterface(serializer, SerializationStrategy) ? serializer : THROW_CCE(), value);
  }
  if (value == null) {
    this.gg();
  } else {
    this.ug();
    this.rg(serializer, value);
  }
}
initMetadataForInterface(Encoder, 'Encoder');
function shouldEncodeElementDefault(descriptor, index) {
  return true;
}
initMetadataForInterface(CompositeEncoder, 'CompositeEncoder');
initMetadataForClass(AbstractEncoder, 'AbstractEncoder', VOID, VOID, [Encoder, CompositeEncoder]);
initMetadataForClass(ListLikeDescriptor, 'ListLikeDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForClass(ArrayListClassDesc, 'ArrayListClassDesc', VOID, ListLikeDescriptor);
initMetadataForClass(AbstractCollectionSerializer, 'AbstractCollectionSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(CollectionLikeSerializer, 'CollectionLikeSerializer', VOID, AbstractCollectionSerializer);
initMetadataForClass(CollectionSerializer, 'CollectionSerializer', VOID, CollectionLikeSerializer);
initMetadataForClass(ArrayListSerializer, 'ArrayListSerializer', VOID, CollectionSerializer);
initMetadataForCompanion(Companion);
initMetadataForClass(ElementMarker, 'ElementMarker');
initMetadataForClass(EnumSerializer, 'EnumSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(PluginGeneratedSerialDescriptor, 'PluginGeneratedSerialDescriptor', VOID, VOID, [SerialDescriptor, CachedNames]);
initMetadataForClass(EnumDescriptor, 'EnumDescriptor', VOID, PluginGeneratedSerialDescriptor);
initMetadataForClass(InlineClassDescriptor, 'InlineClassDescriptor', VOID, PluginGeneratedSerialDescriptor);
function typeParametersSerializers() {
  return get_EMPTY_SERIALIZER_ARRAY();
}
initMetadataForInterface(GeneratedSerializer, 'GeneratedSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(InlinePrimitiveDescriptor$1, VOID, VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(NullableSerializer, 'NullableSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(SerialDescriptorForNullable, 'SerialDescriptorForNullable', VOID, VOID, [SerialDescriptor, CachedNames]);
initMetadataForObject(StringSerializer, 'StringSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(PrimitiveSerialDescriptor, 'PrimitiveSerialDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForObject(IntSerializer, 'IntSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(LongSerializer, 'LongSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(DoubleSerializer, 'DoubleSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(TaggedDecoder, 'TaggedDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
initMetadataForClass(NamedValueDecoder, 'NamedValueDecoder', VOID, TaggedDecoder);
initMetadataForClass(SerializersModule, 'SerializersModule');
initMetadataForClass(SerialModuleImpl, 'SerialModuleImpl', VOID, SerializersModule);
initMetadataForClass(SerializableWith, 'SerializableWith', VOID, VOID, VOID, VOID, 0);
//endregion
function KSerializer() {
}
function DeserializationStrategy() {
}
function SerializationStrategy() {
}
function findPolymorphicSerializer(_this__u8e3s4, encoder, value) {
  var tmp0_elvis_lhs = _this__u8e3s4.ad(encoder, value);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    throwSubtypeNotRegistered(getKClassFromExpression(value), _this__u8e3s4.bd());
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function findPolymorphicSerializer_0(_this__u8e3s4, decoder, klassName) {
  var tmp0_elvis_lhs = _this__u8e3s4.cd(decoder, klassName);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    throwSubtypeNotRegistered_0(klassName, _this__u8e3s4.bd());
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function SealedClassSerializer_init_$Init$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations, $this) {
  SealedClassSerializer.call($this, serialName, baseClass, subclasses, subclassSerializers);
  $this.ed_1 = asList(classAnnotations);
  return $this;
}
function SealedClassSerializer_init_$Create$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations) {
  return SealedClassSerializer_init_$Init$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations, objectCreate(protoOf(SealedClassSerializer)));
}
function SealedClassSerializer$descriptor$delegate$lambda$lambda$lambda(this$0) {
  return function ($this$buildSerialDescriptor) {
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = this$0.hd_1.r().j();
    while (_iterator__ex2g4s.k()) {
      var element = _iterator__ex2g4s.l();
      // Inline function 'kotlin.collections.component1' call
      var name = element.s();
      // Inline function 'kotlin.collections.component2' call
      var serializer = element.t();
      $this$buildSerialDescriptor.qd(name, serializer.xc());
    }
    return Unit_instance;
  };
}
function SealedClassSerializer$descriptor$delegate$lambda$lambda(this$0) {
  return function ($this$buildSerialDescriptor) {
    $this$buildSerialDescriptor.qd('type', serializer(StringCompanionObject_instance).xc());
    var tmp = 'kotlinx.serialization.Sealed<' + this$0.dd_1.x7() + '>';
    var tmp_0 = CONTEXTUAL_getInstance();
    var elementDescriptor = buildSerialDescriptor(tmp, tmp_0, [], SealedClassSerializer$descriptor$delegate$lambda$lambda$lambda(this$0));
    $this$buildSerialDescriptor.qd('value', elementDescriptor);
    $this$buildSerialDescriptor.kd_1 = this$0.ed_1;
    return Unit_instance;
  };
}
function SealedClassSerializer$descriptor$delegate$lambda($serialName, this$0) {
  return function () {
    var tmp = SEALED_getInstance();
    return buildSerialDescriptor($serialName, tmp, [], SealedClassSerializer$descriptor$delegate$lambda$lambda(this$0));
  };
}
function SealedClassSerializer$_get_descriptor_$ref_m511rz() {
  return constructCallableReference(function (p0) {
    return p0.xc();
  }, 1, 0, 0);
}
function SealedClassSerializer$$inlined$groupingBy$1($this) {
  this.rd_1 = $this;
}
protoOf(SealedClassSerializer$$inlined$groupingBy$1).f2 = function () {
  return this.rd_1.j();
};
protoOf(SealedClassSerializer$$inlined$groupingBy$1).sd = function (element) {
  return element.t().xc().td();
};
protoOf(SealedClassSerializer$$inlined$groupingBy$1).g2 = function (element) {
  return this.sd(element);
};
function SealedClassSerializer(serialName, baseClass, subclasses, subclassSerializers) {
  AbstractPolymorphicSerializer.call(this);
  this.dd_1 = baseClass;
  this.ed_1 = emptyList();
  var tmp = this;
  var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp.fd_1 = lazy(tmp_0, SealedClassSerializer$descriptor$delegate$lambda(serialName, this));
  if (!(subclasses.length === subclassSerializers.length)) {
    throw IllegalArgumentException_init_$Create$('All subclasses of sealed class ' + this.dd_1.x7() + ' should be marked @Serializable');
  }
  this.gd_1 = toMap(zip(subclasses, subclassSerializers));
  var tmp_1 = this;
  // Inline function 'kotlin.collections.groupingBy' call
  var this_0 = this.gd_1.r();
  // Inline function 'kotlin.collections.aggregate' call
  var tmp0 = new SealedClassSerializer$$inlined$groupingBy$1(this_0);
  // Inline function 'kotlin.collections.mutableMapOf' call
  // Inline function 'kotlin.collections.aggregateTo' call
  var destination = LinkedHashMap_init_$Create$();
  // Inline function 'kotlin.collections.iterator' call
  var _iterator__ex2g4s = tmp0.f2();
  while (_iterator__ex2g4s.k()) {
    var e = _iterator__ex2g4s.l();
    var key = tmp0.g2(e);
    var accumulator = destination.o1(key);
    accumulator == null && !destination.m1(key);
    if (!(accumulator == null)) {
      // Inline function 'kotlin.error' call
      var message = "Multiple sealed subclasses of '" + toString(this.dd_1) + "' have the same serial name '" + key + "':" + (" '" + toString(accumulator.s()) + "', '" + toString(e.s()) + "'");
      throw IllegalStateException_init_$Create$(toString(message));
    }
    // Inline function 'kotlin.collections.set' call
    destination.h2(key, e);
  }
  // Inline function 'kotlin.collections.mapValues' call
  // Inline function 'kotlin.collections.mapValuesTo' call
  var destination_0 = LinkedHashMap_init_$Create$_0(mapCapacity(destination.o()));
  // Inline function 'kotlin.collections.associateByTo' call
  var _iterator__ex2g4s_0 = destination.r().j();
  while (_iterator__ex2g4s_0.k()) {
    var element = _iterator__ex2g4s_0.l();
    var tmp_2 = element.s();
    var tmp$ret$12 = element.t().t();
    destination_0.h2(tmp_2, tmp$ret$12);
  }
  tmp_1.hd_1 = destination_0;
}
protoOf(SealedClassSerializer).bd = function () {
  return this.dd_1;
};
protoOf(SealedClassSerializer).xc = function () {
  var tmp0 = this.fd_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('descriptor', 1, tmp, SealedClassSerializer$_get_descriptor_$ref_m511rz(), null);
  return tmp0.t();
};
protoOf(SealedClassSerializer).cd = function (decoder, klassName) {
  // Inline function 'kotlin.collections.get' call
  var this_0 = this.hd_1;
  var tmp0_elvis_lhs = (isInterface(this_0, KtMap) ? this_0 : THROW_CCE()).o1(klassName);
  return tmp0_elvis_lhs == null ? protoOf(AbstractPolymorphicSerializer).cd.call(this, decoder, klassName) : tmp0_elvis_lhs;
};
protoOf(SealedClassSerializer).ad = function (encoder, value) {
  var tmp0_elvis_lhs = this.gd_1.o1(getKClassFromExpression(value));
  var tmp1_safe_receiver = tmp0_elvis_lhs == null ? protoOf(AbstractPolymorphicSerializer).ad.call(this, encoder, value) : tmp0_elvis_lhs;
  var tmp;
  if (tmp1_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlinx.serialization.internal.cast' call
    tmp = isInterface(tmp1_safe_receiver, SerializationStrategy) ? tmp1_safe_receiver : THROW_CCE();
  }
  return tmp;
};
function MissingFieldException_init_$Init$(missingFields, serialName, $this) {
  var tmp0_message = missingFields.o() === 1 ? "Field '" + missingFields.n(0) + "' is required for type with serial name '" + serialName + "', but it was missing" : 'Fields ' + toString(missingFields) + " are required for type with serial name '" + serialName + "', but they were missing";
  MissingFieldException.call($this, tmp0_message, null, missingFields, serialName);
  return $this;
}
function MissingFieldException_init_$Create$(missingFields, serialName) {
  var tmp = MissingFieldException_init_$Init$(missingFields, serialName, objectCreate(protoOf(MissingFieldException)));
  captureStack(tmp, MissingFieldException_init_$Create$);
  return tmp;
}
function MissingFieldException(message, cause, missingFields, serialName) {
  SerializationException_init_$Init$_1(message, cause, this);
  captureStack(this, MissingFieldException);
  this.vd_1 = missingFields;
  this.wd_1 = serialName;
}
protoOf(MissingFieldException).xd = function (newMessage) {
  return new MissingFieldException(newMessage, this, this.vd_1, this.wd_1);
};
function SerializationException_init_$Init$($this) {
  IllegalArgumentException_init_$Init$($this);
  SerializationException.call($this);
  return $this;
}
function SerializationException_init_$Create$() {
  var tmp = SerializationException_init_$Init$(objectCreate(protoOf(SerializationException)));
  captureStack(tmp, SerializationException_init_$Create$);
  return tmp;
}
function SerializationException_init_$Init$_0(message, $this) {
  IllegalArgumentException_init_$Init$_0(message, $this);
  SerializationException.call($this);
  return $this;
}
function SerializationException_init_$Create$_0(message) {
  var tmp = SerializationException_init_$Init$_0(message, objectCreate(protoOf(SerializationException)));
  captureStack(tmp, SerializationException_init_$Create$_0);
  return tmp;
}
function SerializationException_init_$Init$_1(message, cause, $this) {
  IllegalArgumentException_init_$Init$_1(message, cause, $this);
  SerializationException.call($this);
  return $this;
}
function SerializationException() {
  captureStack(this, SerializationException);
}
function UnknownFieldException_init_$Init$(index, $this) {
  UnknownFieldException.call($this, 'An unknown field for index ' + index);
  return $this;
}
function UnknownFieldException_init_$Create$(index) {
  var tmp = UnknownFieldException_init_$Init$(index, objectCreate(protoOf(UnknownFieldException)));
  captureStack(tmp, UnknownFieldException_init_$Create$);
  return tmp;
}
function UnknownFieldException(message) {
  SerializationException_init_$Init$_0(message, this);
  captureStack(this, UnknownFieldException);
}
function serializer(_this__u8e3s4) {
  return StringSerializer_getInstance();
}
function get_nullable(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4.xc().yd()) {
    tmp = isInterface(_this__u8e3s4, KSerializer) ? _this__u8e3s4 : THROW_CCE();
  } else {
    tmp = new NullableSerializer(_this__u8e3s4);
  }
  return tmp;
}
function ContextDescriptor() {
}
function getContextualDescriptor(_this__u8e3s4, descriptor) {
  var tmp0_safe_receiver = get_capturedKClass(descriptor);
  var tmp;
  if (tmp0_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlin.let' call
    var tmp0_safe_receiver_0 = _this__u8e3s4.zd(tmp0_safe_receiver);
    tmp = tmp0_safe_receiver_0 == null ? null : tmp0_safe_receiver_0.xc();
  }
  return tmp;
}
function get_capturedKClass(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4 instanceof ContextDescriptor) {
    tmp = _this__u8e3s4.de_1;
  } else {
    if (_this__u8e3s4 instanceof SerialDescriptorForNullable) {
      tmp = get_capturedKClass(_this__u8e3s4.ae_1);
    } else {
      tmp = null;
    }
  }
  return tmp;
}
function SerialDescriptor() {
}
function get_elementDescriptors(_this__u8e3s4) {
  // Inline function 'kotlin.collections.Iterable' call
  return new elementDescriptors$$inlined$Iterable$1(_this__u8e3s4);
}
function get_elementNames(_this__u8e3s4) {
  // Inline function 'kotlin.collections.Iterable' call
  return new elementNames$$inlined$Iterable$1(_this__u8e3s4);
}
function elementDescriptors$1($this_elementDescriptors) {
  this.oe_1 = $this_elementDescriptors;
  this.ne_1 = $this_elementDescriptors.ge();
}
protoOf(elementDescriptors$1).k = function () {
  return this.ne_1 > 0;
};
protoOf(elementDescriptors$1).l = function () {
  var tmp = this.oe_1.ge();
  var _unary__edvuaz = this.ne_1;
  this.ne_1 = _unary__edvuaz - 1 | 0;
  return this.oe_1.le(tmp - _unary__edvuaz | 0);
};
function elementDescriptors$$inlined$Iterable$1($this_elementDescriptors) {
  this.pe_1 = $this_elementDescriptors;
}
protoOf(elementDescriptors$$inlined$Iterable$1).j = function () {
  return new elementDescriptors$1(this.pe_1);
};
function elementNames$1($this_elementNames) {
  this.re_1 = $this_elementNames;
  this.qe_1 = $this_elementNames.ge();
}
protoOf(elementNames$1).k = function () {
  return this.qe_1 > 0;
};
protoOf(elementNames$1).l = function () {
  var tmp = this.re_1.ge();
  var _unary__edvuaz = this.qe_1;
  this.qe_1 = _unary__edvuaz - 1 | 0;
  return this.re_1.ie(tmp - _unary__edvuaz | 0);
};
function elementNames$$inlined$Iterable$1($this_elementNames) {
  this.se_1 = $this_elementNames;
}
protoOf(elementNames$$inlined$Iterable$1).j = function () {
  return new elementNames$1(this.se_1);
};
function ClassSerialDescriptorBuilder(serialName) {
  this.id_1 = serialName;
  this.jd_1 = false;
  this.kd_1 = emptyList();
  this.ld_1 = ArrayList_init_$Create$();
  this.md_1 = HashSet_init_$Create$();
  this.nd_1 = ArrayList_init_$Create$();
  this.od_1 = ArrayList_init_$Create$();
  this.pd_1 = ArrayList_init_$Create$();
}
protoOf(ClassSerialDescriptorBuilder).te = function (elementName, descriptor, annotations, isOptional) {
  // Inline function 'kotlin.require' call
  if (!this.md_1.i(elementName)) {
    var message = "Element with name '" + elementName + "' is already registered in " + this.id_1;
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  // Inline function 'kotlin.collections.plusAssign' call
  this.ld_1.i(elementName);
  // Inline function 'kotlin.collections.plusAssign' call
  this.nd_1.i(descriptor);
  // Inline function 'kotlin.collections.plusAssign' call
  this.od_1.i(annotations);
  // Inline function 'kotlin.collections.plusAssign' call
  this.pd_1.i(isOptional);
};
protoOf(ClassSerialDescriptorBuilder).qd = function (elementName, descriptor, annotations, isOptional, $super) {
  annotations = annotations === VOID ? emptyList() : annotations;
  isOptional = isOptional === VOID ? false : isOptional;
  var tmp;
  if ($super === VOID) {
    this.te(elementName, descriptor, annotations, isOptional);
    tmp = Unit_instance;
  } else {
    tmp = $super.te.call(this, elementName, descriptor, annotations, isOptional);
  }
  return tmp;
};
function buildSerialDescriptor(serialName, kind, typeParameters, builder) {
  var tmp;
  if (builder === VOID) {
    tmp = buildSerialDescriptor$lambda;
  } else {
    tmp = builder;
  }
  builder = tmp;
  // Inline function 'kotlin.text.isNotBlank' call
  // Inline function 'kotlin.require' call
  if (!!isBlank(serialName)) {
    var message = 'Blank serial names are prohibited';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  // Inline function 'kotlin.require' call
  if (!!equals(kind, CLASS_getInstance())) {
    var message_0 = "For StructureKind.CLASS please use 'buildClassSerialDescriptor' instead";
    throw IllegalArgumentException_init_$Create$(toString(message_0));
  }
  var sdBuilder = new ClassSerialDescriptorBuilder(serialName);
  builder(sdBuilder);
  return new SerialDescriptorImpl(serialName, kind, sdBuilder.ld_1.o(), toList(typeParameters), sdBuilder);
}
function _get__hashCode__tgwhef($this) {
  var tmp0 = $this.ff_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp(), null);
  return tmp0.t();
}
function SerialDescriptorImpl$_hashCode$delegate$lambda(this$0) {
  return function () {
    return hashCodeImpl(this$0, this$0.ef_1);
  };
}
function SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp() {
  return constructCallableReference(function (p0) {
    return _get__hashCode__tgwhef(p0);
  }, 1, 0, 1);
}
function SerialDescriptorImpl(serialName, kind, elementsCount, typeParameters, builder) {
  this.ue_1 = serialName;
  this.ve_1 = kind;
  this.we_1 = elementsCount;
  this.xe_1 = builder.kd_1;
  this.ye_1 = toHashSet(builder.ld_1);
  var tmp = this;
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = builder.ld_1;
  tmp.ze_1 = copyToArray(this_0);
  this.af_1 = compactArray(builder.nd_1);
  var tmp_0 = this;
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_1 = builder.od_1;
  tmp_0.bf_1 = copyToArray(this_1);
  this.cf_1 = toBooleanArray(builder.pd_1);
  var tmp_1 = this;
  // Inline function 'kotlin.collections.map' call
  var this_2 = withIndex(this.ze_1);
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$_0(collectionSizeOrDefault(this_2, 10));
  var _iterator__ex2g4s = this_2.j();
  while (_iterator__ex2g4s.k()) {
    var item = _iterator__ex2g4s.l();
    var tmp$ret$4 = to(item.ca_1, item.ba_1);
    destination.i(tmp$ret$4);
  }
  tmp_1.df_1 = toMap(destination);
  this.ef_1 = compactArray(typeParameters);
  var tmp_2 = this;
  tmp_2.ff_1 = lazy_0(SerialDescriptorImpl$_hashCode$delegate$lambda(this));
}
protoOf(SerialDescriptorImpl).td = function () {
  return this.ue_1;
};
protoOf(SerialDescriptorImpl).ee = function () {
  return this.ve_1;
};
protoOf(SerialDescriptorImpl).ge = function () {
  return this.we_1;
};
protoOf(SerialDescriptorImpl).he = function () {
  return this.xe_1;
};
protoOf(SerialDescriptorImpl).gf = function () {
  return this.ye_1;
};
protoOf(SerialDescriptorImpl).ie = function (index) {
  return getChecked(this.ze_1, index);
};
protoOf(SerialDescriptorImpl).je = function (name) {
  var tmp0_elvis_lhs = this.df_1.o1(name);
  return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
};
protoOf(SerialDescriptorImpl).ke = function (index) {
  return getChecked(this.bf_1, index);
};
protoOf(SerialDescriptorImpl).le = function (index) {
  return getChecked(this.af_1, index);
};
protoOf(SerialDescriptorImpl).me = function (index) {
  return getChecked_0(this.cf_1, index);
};
protoOf(SerialDescriptorImpl).equals = function (other) {
  var tmp$ret$0;
  $l$block_5: {
    // Inline function 'kotlinx.serialization.internal.equalsImpl' call
    if (this === other) {
      tmp$ret$0 = true;
      break $l$block_5;
    }
    if (!(other instanceof SerialDescriptorImpl)) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.td() === other.td())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!contentEquals(this.ef_1, other.ef_1)) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.ge() === other.ge())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.ge();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.le(index).td() === other.le(index).td())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.le(index).ee(), other.le(index).ee())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
      }
       while (inductionVariable < last);
    tmp$ret$0 = true;
  }
  return tmp$ret$0;
};
protoOf(SerialDescriptorImpl).hashCode = function () {
  return _get__hashCode__tgwhef(this);
};
protoOf(SerialDescriptorImpl).toString = function () {
  return toStringImpl(this);
};
function buildSerialDescriptor$lambda(_this__u8e3s4) {
  return Unit_instance;
}
function INT() {
  INT_instance = this;
  PrimitiveKind.call(this);
}
var INT_instance;
function INT_getInstance() {
  if (INT_instance == null)
    new INT();
  return INT_instance;
}
function LONG() {
  LONG_instance = this;
  PrimitiveKind.call(this);
}
var LONG_instance;
function LONG_getInstance() {
  if (LONG_instance == null)
    new LONG();
  return LONG_instance;
}
function DOUBLE() {
  DOUBLE_instance = this;
  PrimitiveKind.call(this);
}
var DOUBLE_instance;
function DOUBLE_getInstance() {
  if (DOUBLE_instance == null)
    new DOUBLE();
  return DOUBLE_instance;
}
function STRING() {
  STRING_instance = this;
  PrimitiveKind.call(this);
}
var STRING_instance;
function STRING_getInstance() {
  if (STRING_instance == null)
    new STRING();
  return STRING_instance;
}
function PrimitiveKind() {
  SerialKind.call(this);
}
function SEALED() {
  SEALED_instance = this;
  PolymorphicKind.call(this);
}
var SEALED_instance;
function SEALED_getInstance() {
  if (SEALED_instance == null)
    new SEALED();
  return SEALED_instance;
}
function PolymorphicKind() {
  SerialKind.call(this);
}
function ENUM() {
  ENUM_instance = this;
  SerialKind.call(this);
}
var ENUM_instance;
function ENUM_getInstance() {
  if (ENUM_instance == null)
    new ENUM();
  return ENUM_instance;
}
function CONTEXTUAL() {
  CONTEXTUAL_instance = this;
  SerialKind.call(this);
}
var CONTEXTUAL_instance;
function CONTEXTUAL_getInstance() {
  if (CONTEXTUAL_instance == null)
    new CONTEXTUAL();
  return CONTEXTUAL_instance;
}
function SerialKind() {
}
protoOf(SerialKind).toString = function () {
  return ensureNotNull(getKClassFromExpression(this).x7());
};
protoOf(SerialKind).hashCode = function () {
  return getStringHashCode(this.toString());
};
function CLASS() {
  CLASS_instance = this;
  StructureKind.call(this);
}
var CLASS_instance;
function CLASS_getInstance() {
  if (CLASS_instance == null)
    new CLASS();
  return CLASS_instance;
}
function LIST() {
  LIST_instance = this;
  StructureKind.call(this);
}
var LIST_instance;
function LIST_getInstance() {
  if (LIST_instance == null)
    new LIST();
  return LIST_instance;
}
function MAP() {
  MAP_instance = this;
  StructureKind.call(this);
}
var MAP_instance;
function MAP_getInstance() {
  if (MAP_instance == null)
    new MAP();
  return MAP_instance;
}
function OBJECT() {
  OBJECT_instance = this;
  StructureKind.call(this);
}
var OBJECT_instance;
function OBJECT_getInstance() {
  if (OBJECT_instance == null)
    new OBJECT();
  return OBJECT_instance;
}
function StructureKind() {
  SerialKind.call(this);
}
function AbstractDecoder() {
}
protoOf(AbstractDecoder).hf = function () {
  throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
};
protoOf(AbstractDecoder).if = function () {
  return true;
};
protoOf(AbstractDecoder).jf = function () {
  return null;
};
protoOf(AbstractDecoder).kf = function () {
  var tmp = this.hf();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).lf = function () {
  var tmp = this.hf();
  return tmp instanceof Long ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).mf = function () {
  var tmp = this.hf();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).nf = function () {
  var tmp = this.hf();
  return typeof tmp === 'string' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).of = function (enumDescriptor) {
  var tmp = this.hf();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).pf = function (deserializer, previousValue) {
  return this.qf(deserializer);
};
protoOf(AbstractDecoder).rf = function (descriptor) {
  return this;
};
protoOf(AbstractDecoder).sf = function (descriptor) {
};
protoOf(AbstractDecoder).tf = function (descriptor, index) {
  return this.kf();
};
protoOf(AbstractDecoder).uf = function (descriptor, index) {
  return this.lf();
};
protoOf(AbstractDecoder).vf = function (descriptor, index) {
  return this.mf();
};
protoOf(AbstractDecoder).wf = function (descriptor, index) {
  return this.nf();
};
protoOf(AbstractDecoder).xf = function (descriptor, index, deserializer, previousValue) {
  return this.pf(deserializer, previousValue);
};
protoOf(AbstractDecoder).zf = function (descriptor, index, deserializer, previousValue) {
  // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
  var isNullabilitySupported = deserializer.xc().yd();
  var tmp;
  if (isNullabilitySupported || this.if()) {
    tmp = this.pf(deserializer, previousValue);
  } else {
    tmp = this.jf();
  }
  return tmp;
};
function AbstractEncoder() {
}
protoOf(AbstractEncoder).rf = function (descriptor) {
  return this;
};
protoOf(AbstractEncoder).sf = function (descriptor) {
};
protoOf(AbstractEncoder).eg = function (descriptor, index) {
  return true;
};
protoOf(AbstractEncoder).fg = function (value) {
  throw SerializationException_init_$Create$_0('Non-serializable ' + toString(getKClassFromExpression(value)) + ' is not supported by ' + toString(getKClassFromExpression(this)) + ' encoder');
};
protoOf(AbstractEncoder).gg = function () {
  throw SerializationException_init_$Create$_0("'null' is not supported by default");
};
protoOf(AbstractEncoder).hg = function (value) {
  return this.fg(value);
};
protoOf(AbstractEncoder).ig = function (value) {
  return this.fg(value);
};
protoOf(AbstractEncoder).jg = function (value) {
  return this.fg(value);
};
protoOf(AbstractEncoder).kg = function (value) {
  return this.fg(value);
};
protoOf(AbstractEncoder).lg = function (enumDescriptor, index) {
  return this.fg(index);
};
protoOf(AbstractEncoder).mg = function (descriptor, index, value) {
  if (this.eg(descriptor, index)) {
    this.hg(value);
  }
};
protoOf(AbstractEncoder).ng = function (descriptor, index, value) {
  if (this.eg(descriptor, index)) {
    this.ig(value);
  }
};
protoOf(AbstractEncoder).og = function (descriptor, index, value) {
  if (this.eg(descriptor, index)) {
    this.jg(value);
  }
};
protoOf(AbstractEncoder).pg = function (descriptor, index, value) {
  if (this.eg(descriptor, index)) {
    this.kg(value);
  }
};
protoOf(AbstractEncoder).qg = function (descriptor, index, serializer, value) {
  if (this.eg(descriptor, index)) {
    this.rg(serializer, value);
  }
};
protoOf(AbstractEncoder).sg = function (descriptor, index, serializer, value) {
  if (this.eg(descriptor, index)) {
    this.tg(serializer, value);
  }
};
function Decoder() {
}
function CompositeDecoder() {
}
function Encoder() {
}
function CompositeEncoder() {
}
function decodeSequentially_0($this, compositeDecoder) {
  var klassName = compositeDecoder.wf($this.xc(), 0);
  var serializer = findPolymorphicSerializer_0($this, compositeDecoder, klassName);
  return compositeDecoder.yf($this.xc(), 1, serializer);
}
function AbstractPolymorphicSerializer() {
}
protoOf(AbstractPolymorphicSerializer).ud = function (encoder, value) {
  var actualSerializer = findPolymorphicSerializer(this, encoder, value);
  // Inline function 'kotlinx.serialization.encoding.encodeStructure' call
  var descriptor = this.xc();
  var composite = encoder.rf(descriptor);
  composite.pg(this.xc(), 0, actualSerializer.xc().td());
  var tmp = this.xc();
  // Inline function 'kotlinx.serialization.internal.cast' call
  var tmp$ret$2 = isInterface(actualSerializer, SerializationStrategy) ? actualSerializer : THROW_CCE();
  composite.qg(tmp, 1, tmp$ret$2, value);
  composite.sf(descriptor);
};
protoOf(AbstractPolymorphicSerializer).zc = function (encoder, value) {
  return this.ud(encoder, !(value == null) ? value : THROW_CCE());
};
protoOf(AbstractPolymorphicSerializer).yc = function (decoder) {
  // Inline function 'kotlinx.serialization.encoding.decodeStructure' call
  var descriptor = this.xc();
  var composite = decoder.rf(descriptor);
  var tmp$ret$1;
  $l$block: {
    var klassName = null;
    var value = null;
    if (composite.bg()) {
      tmp$ret$1 = decodeSequentially_0(this, composite);
      break $l$block;
    }
    mainLoop: while (true) {
      var index = composite.cg(this.xc());
      switch (index) {
        case -1:
          break mainLoop;
        case 0:
          klassName = composite.wf(this.xc(), index);
          break;
        case 1:
          var tmp0 = klassName;
          var tmp$ret$2;
          $l$block_0: {
            // Inline function 'kotlin.requireNotNull' call
            if (tmp0 == null) {
              var message = 'Cannot read polymorphic value before its type token';
              throw IllegalArgumentException_init_$Create$(toString(message));
            } else {
              tmp$ret$2 = tmp0;
              break $l$block_0;
            }
          }

          klassName = tmp$ret$2;
          var serializer = findPolymorphicSerializer_0(this, composite, klassName);
          value = composite.yf(this.xc(), index, serializer);
          break;
        default:
          var tmp0_elvis_lhs = klassName;
          throw SerializationException_init_$Create$_0('Invalid index in polymorphic deserialization of ' + (tmp0_elvis_lhs == null ? 'unknown class' : tmp0_elvis_lhs) + ('\n Expected 0, 1 or DECODE_DONE(-1), but found ' + index));
      }
    }
    var tmp0_0 = value;
    var tmp$ret$4;
    $l$block_1: {
      // Inline function 'kotlin.requireNotNull' call
      if (tmp0_0 == null) {
        var message_0 = 'Polymorphic value has not been read for class ' + klassName;
        throw IllegalArgumentException_init_$Create$(toString(message_0));
      } else {
        tmp$ret$4 = tmp0_0;
        break $l$block_1;
      }
    }
    var tmp = tmp$ret$4;
    tmp$ret$1 = !(tmp == null) ? tmp : THROW_CCE();
  }
  var result = tmp$ret$1;
  composite.sf(descriptor);
  return result;
};
protoOf(AbstractPolymorphicSerializer).cd = function (decoder, klassName) {
  return decoder.ag().xg(this.bd(), klassName);
};
protoOf(AbstractPolymorphicSerializer).ad = function (encoder, value) {
  return encoder.ag().yg(this.bd(), value);
};
function throwSubtypeNotRegistered(subClass, baseClass) {
  var tmp0_elvis_lhs = subClass.x7();
  throwSubtypeNotRegistered_0(tmp0_elvis_lhs == null ? toString(subClass) : tmp0_elvis_lhs, baseClass);
}
function throwSubtypeNotRegistered_0(subClassName, baseClass) {
  var scope = "in the polymorphic scope of '" + baseClass.x7() + "'";
  throw SerializationException_init_$Create$_0(subClassName == null ? 'Class discriminator was missing and no default serializers were registered ' + scope + '.' : "Serializer for subclass '" + subClassName + "' is not found " + scope + '.\n' + ("Check if class with serial name '" + subClassName + "' exists and serializer is registered in a corresponding SerializersModule.\n") + ("To be registered automatically, class '" + subClassName + "' has to be '@Serializable', and the base class '" + baseClass.x7() + "' has to be sealed and '@Serializable'."));
}
function CachedNames() {
}
function ArrayListClassDesc(elementDesc) {
  ListLikeDescriptor.call(this, elementDesc);
}
protoOf(ArrayListClassDesc).td = function () {
  return 'kotlin.collections.ArrayList';
};
function ListLikeDescriptor(elementDescriptor) {
  this.bh_1 = elementDescriptor;
  this.ch_1 = 1;
}
protoOf(ListLikeDescriptor).ee = function () {
  return LIST_getInstance();
};
protoOf(ListLikeDescriptor).ge = function () {
  return this.ch_1;
};
protoOf(ListLikeDescriptor).ie = function (index) {
  requireNonNegativeIndex(index, this.td());
  return index.toString();
};
protoOf(ListLikeDescriptor).je = function (name) {
  var tmp0_safe_receiver = toIntOrNull(name);
  var tmp;
  if (tmp0_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlin.takeIf' call
    var tmp_0;
    if (tmp0_safe_receiver >= 0) {
      tmp_0 = tmp0_safe_receiver;
    } else {
      tmp_0 = null;
    }
    tmp = tmp_0;
  }
  var tmp1_elvis_lhs = tmp;
  return tmp1_elvis_lhs == null ? -3 : tmp1_elvis_lhs;
};
protoOf(ListLikeDescriptor).me = function (index) {
  requireNonNegativeIndex(index, this.td());
  return false;
};
protoOf(ListLikeDescriptor).ke = function (index) {
  requireNonNegativeIndex(index, this.td());
  return emptyList();
};
protoOf(ListLikeDescriptor).le = function (index) {
  requireNonNegativeIndex(index, this.td());
  return this.bh_1;
};
protoOf(ListLikeDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof ListLikeDescriptor))
    return false;
  if (equals(this.bh_1, other.bh_1) && this.td() === other.td())
    return true;
  return false;
};
protoOf(ListLikeDescriptor).hashCode = function () {
  return imul(hashCode(this.bh_1), 31) + getStringHashCode(this.td()) | 0;
};
protoOf(ListLikeDescriptor).toString = function () {
  return this.td() + '(' + toString(this.bh_1) + ')';
};
function requireNonNegativeIndex(index, name) {
  if (index < 0)
    throw IndexOutOfBoundsException_init_$Create$('Illegal index ' + index + ', ' + name + ' expects only non-negative indices');
}
function ArrayListSerializer(element) {
  CollectionSerializer.call(this, element);
  this.eh_1 = new ArrayListClassDesc(element.xc());
}
protoOf(ArrayListSerializer).xc = function () {
  return this.eh_1;
};
protoOf(ArrayListSerializer).fh = function (initialCapacityHint) {
  if (initialCapacityHint === -1) {
    // Inline function 'kotlin.collections.arrayListOf' call
    return ArrayList_init_$Create$();
  }
  return ArrayList_init_$Create$_0(initialCapacityHint);
};
protoOf(ArrayListSerializer).gh = function (_this__u8e3s4) {
  return _this__u8e3s4.o();
};
protoOf(ArrayListSerializer).hh = function (_this__u8e3s4) {
  return this.gh(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
};
protoOf(ArrayListSerializer).ih = function (_this__u8e3s4) {
  return _this__u8e3s4;
};
protoOf(ArrayListSerializer).jh = function (_this__u8e3s4) {
  return this.ih(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
};
protoOf(ArrayListSerializer).kh = function (_this__u8e3s4, expectedSize) {
  if (_this__u8e3s4 instanceof ArrayList) {
    // Inline function 'kotlin.also' call
    _this__u8e3s4.s3(expectedSize);
    return _this__u8e3s4;
  }
  if (expectedSize === _this__u8e3s4.o())
    return ArrayList_init_$Create$_1(_this__u8e3s4);
  // Inline function 'kotlin.also' call
  var this_0 = ArrayList_init_$Create$_0(expectedSize);
  this_0.p(_this__u8e3s4);
  return this_0;
};
protoOf(ArrayListSerializer).lh = function (_this__u8e3s4, expectedSize) {
  return this.kh((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtList) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
};
protoOf(ArrayListSerializer).mh = function (_this__u8e3s4, index, element) {
  _this__u8e3s4.z2(index, element);
};
protoOf(ArrayListSerializer).nh = function (_this__u8e3s4, index, element) {
  return this.mh(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE(), index, element);
};
function CollectionSerializer(element) {
  CollectionLikeSerializer.call(this, element);
}
protoOf(CollectionSerializer).ph = function (_this__u8e3s4) {
  return _this__u8e3s4.o();
};
protoOf(CollectionSerializer).xh = function (_this__u8e3s4) {
  return this.ph((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
};
protoOf(CollectionSerializer).qh = function (_this__u8e3s4) {
  return _this__u8e3s4.j();
};
protoOf(CollectionSerializer).yh = function (_this__u8e3s4) {
  return this.qh((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
};
function CollectionLikeSerializer(elementSerializer) {
  AbstractCollectionSerializer.call(this);
  this.rh_1 = elementSerializer;
}
protoOf(CollectionLikeSerializer).sh = function (encoder, value) {
  var size = this.xh(value);
  // Inline function 'kotlinx.serialization.encoding.encodeCollection' call
  var descriptor = this.xc();
  var composite = encoder.vg(descriptor, size);
  var iterator = this.yh(value);
  var inductionVariable = 0;
  if (inductionVariable < size)
    do {
      var index = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      composite.qg(this.xc(), index, this.rh_1, iterator.l());
    }
     while (inductionVariable < size);
  composite.sf(descriptor);
};
protoOf(CollectionLikeSerializer).zc = function (encoder, value) {
  return this.sh(encoder, value);
};
protoOf(CollectionLikeSerializer).th = function (decoder, builder, startIndex, size) {
  // Inline function 'kotlin.require' call
  if (!(size >= 0)) {
    var message = 'Size must be known in advance when using READ_ALL';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  var inductionVariable = 0;
  if (inductionVariable < size)
    do {
      var index = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      this.uh(decoder, startIndex + index | 0, builder, false);
    }
     while (inductionVariable < size);
};
protoOf(CollectionLikeSerializer).uh = function (decoder, index, builder, checkIndex) {
  this.nh(builder, index, decoder.yf(this.xc(), index, this.rh_1));
};
function AbstractCollectionSerializer() {
}
protoOf(AbstractCollectionSerializer).wh = function (decoder, previous) {
  var compositeDecoder = decoder.rf(this.xc());
  var expectedSize = compositeDecoder.dg(this.xc());
  // Inline function 'kotlin.require' call
  if (!(expectedSize >= -1)) {
    var message = 'Decoded collection size should be either non-negative, or -1 (meaning, unknown). Was: ' + expectedSize;
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  var tmp;
  if (!(previous == null)) {
    tmp = this.lh(previous, this.xh(previous) + coerceAtLeast(expectedSize, 0) | 0);
  } else {
    tmp = this.fh(expectedSize);
  }
  var builder = tmp;
  var startIndex = this.hh(builder);
  if (compositeDecoder.bg()) {
    this.th(compositeDecoder, builder, startIndex, expectedSize);
  } else {
    $l$loop: while (true) {
      var index = compositeDecoder.cg(this.xc());
      if (index === -1)
        break $l$loop;
      this.vh(compositeDecoder, startIndex + index | 0, builder);
    }
  }
  compositeDecoder.sf(this.xc());
  return this.jh(builder);
};
protoOf(AbstractCollectionSerializer).yc = function (decoder) {
  return this.wh(decoder, null);
};
protoOf(AbstractCollectionSerializer).vh = function (decoder, index, builder, checkIndex, $super) {
  checkIndex = checkIndex === VOID ? true : checkIndex;
  var tmp;
  if ($super === VOID) {
    this.uh(decoder, index, builder, checkIndex);
    tmp = Unit_instance;
  } else {
    tmp = $super.uh.call(this, decoder, index, builder, checkIndex);
  }
  return tmp;
};
function Companion() {
  Companion_instance = this;
  this.zh_1 = longArray(0);
}
var Companion_instance;
function Companion_getInstance() {
  if (Companion_instance == null)
    new Companion();
  return Companion_instance;
}
function prepareHighMarksArray($this, elementsCount) {
  var slotsCount = (elementsCount - 1 | 0) >>> 6 | 0;
  var elementsInLastSlot = elementsCount & 63;
  var highMarks = longArray(slotsCount);
  if (!(elementsInLastSlot === 0)) {
    highMarks[get_lastIndex(highMarks)] = shiftLeft(new Long(-1, -1), elementsCount);
  }
  return highMarks;
}
function markHigh($this, index) {
  var slot = (index >>> 6 | 0) - 1 | 0;
  var offsetInSlot = index & 63;
  $this.di_1[slot] = bitwiseOr($this.di_1[slot], shiftLeft(new Long(1, 0), offsetInSlot));
}
function nextUnmarkedHighIndex($this) {
  var inductionVariable = 0;
  var last = $this.di_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var slot = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      var slotOffset = imul(slot + 1 | 0, 64);
      var slotMarks = $this.di_1[slot];
      while (!equalsLong(slotMarks, new Long(-1, -1))) {
        var indexInSlot = countTrailingZeroBits(invert(slotMarks));
        slotMarks = bitwiseOr(slotMarks, shiftLeft(new Long(1, 0), indexInSlot));
        var index = slotOffset + indexInSlot | 0;
        if ($this.bi_1($this.ai_1, index)) {
          $this.di_1[slot] = slotMarks;
          return index;
        }
      }
      $this.di_1[slot] = slotMarks;
    }
     while (inductionVariable <= last);
  return -1;
}
function ElementMarker(descriptor, readIfAbsent) {
  Companion_getInstance();
  this.ai_1 = descriptor;
  this.bi_1 = readIfAbsent;
  var elementsCount = this.ai_1.ge();
  if (elementsCount <= 64) {
    var tmp = this;
    var tmp_0;
    if (elementsCount === 64) {
      tmp_0 = new Long(0, 0);
    } else {
      tmp_0 = shiftLeft(new Long(-1, -1), elementsCount);
    }
    tmp.ci_1 = tmp_0;
    this.di_1 = Companion_getInstance().zh_1;
  } else {
    this.ci_1 = new Long(0, 0);
    this.di_1 = prepareHighMarksArray(this, elementsCount);
  }
}
protoOf(ElementMarker).ei = function (index) {
  if (index < 64) {
    this.ci_1 = bitwiseOr(this.ci_1, shiftLeft(new Long(1, 0), index));
  } else {
    markHigh(this, index);
  }
};
protoOf(ElementMarker).fi = function () {
  var elementsCount = this.ai_1.ge();
  while (!equalsLong(this.ci_1, new Long(-1, -1))) {
    var index = countTrailingZeroBits(invert(this.ci_1));
    this.ci_1 = bitwiseOr(this.ci_1, shiftLeft(new Long(1, 0), index));
    if (this.bi_1(this.ai_1, index)) {
      return index;
    }
  }
  if (elementsCount > 64) {
    return nextUnmarkedHighIndex(this);
  }
  return -1;
};
function createSimpleEnumSerializer(serialName, values) {
  return new EnumSerializer(serialName, values);
}
function createUnmarkedDescriptor($this, serialName) {
  var d = new EnumDescriptor(serialName, $this.gi_1.length);
  // Inline function 'kotlin.collections.forEach' call
  var indexedObject = $this.gi_1;
  var inductionVariable = 0;
  var last = indexedObject.length;
  while (inductionVariable < last) {
    var element = indexedObject[inductionVariable];
    inductionVariable = inductionVariable + 1 | 0;
    d.vi(element.r1_1);
  }
  return d;
}
function EnumSerializer$descriptor$delegate$lambda(this$0, $serialName) {
  return function () {
    var tmp0_elvis_lhs = this$0.hi_1;
    return tmp0_elvis_lhs == null ? createUnmarkedDescriptor(this$0, $serialName) : tmp0_elvis_lhs;
  };
}
function EnumSerializer$_get_descriptor_$ref_j67dlw() {
  return constructCallableReference(function (p0) {
    return p0.xc();
  }, 1, 0, 2);
}
function EnumSerializer(serialName, values) {
  this.gi_1 = values;
  this.hi_1 = null;
  var tmp = this;
  tmp.ii_1 = lazy_0(EnumSerializer$descriptor$delegate$lambda(this, serialName));
}
protoOf(EnumSerializer).xc = function () {
  var tmp0 = this.ii_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('descriptor', 1, tmp, EnumSerializer$_get_descriptor_$ref_j67dlw(), null);
  return tmp0.t();
};
protoOf(EnumSerializer).wi = function (encoder, value) {
  var index = indexOf(this.gi_1, value);
  if (index === -1) {
    throw SerializationException_init_$Create$_0(toString(value) + ' is not a valid enum ' + this.xc().td() + ', ' + ('must be one of ' + contentToString(this.gi_1)));
  }
  encoder.lg(this.xc(), index);
};
protoOf(EnumSerializer).zc = function (encoder, value) {
  return this.wi(encoder, value instanceof Enum ? value : THROW_CCE());
};
protoOf(EnumSerializer).yc = function (decoder) {
  var index = decoder.of(this.xc());
  if (!(0 <= index ? index <= (this.gi_1.length - 1 | 0) : false)) {
    throw SerializationException_init_$Create$_0('' + index + ' is not among valid ' + this.xc().td() + ' enum values, ' + ('values size is ' + this.gi_1.length));
  }
  return this.gi_1[index];
};
protoOf(EnumSerializer).toString = function () {
  return 'kotlinx.serialization.internal.EnumSerializer<' + this.xc().td() + '>';
};
function _get_elementDescriptors__y23q9p($this) {
  var tmp0 = $this.kj_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('elementDescriptors', 1, tmp, EnumDescriptor$_get_elementDescriptors_$ref_5lvk4a(), null);
  return tmp0.t();
}
function EnumDescriptor$elementDescriptors$delegate$lambda($elementsCount, $name, this$0) {
  return function () {
    var tmp = 0;
    var tmp_0 = $elementsCount;
    // Inline function 'kotlin.arrayOfNulls' call
    var tmp_1 = Array(tmp_0);
    while (tmp < tmp_0) {
      var tmp_2 = tmp;
      tmp_1[tmp_2] = buildSerialDescriptor($name + '.' + this$0.ie(tmp_2), OBJECT_getInstance(), []);
      tmp = tmp + 1 | 0;
    }
    return tmp_1;
  };
}
function EnumDescriptor$_get_elementDescriptors_$ref_5lvk4a() {
  return constructCallableReference(function (p0) {
    return _get_elementDescriptors__y23q9p(p0);
  }, 1, 0, 3);
}
function EnumDescriptor(name, elementsCount) {
  PluginGeneratedSerialDescriptor.call(this, name, VOID, elementsCount);
  this.jj_1 = ENUM_getInstance();
  var tmp = this;
  tmp.kj_1 = lazy_0(EnumDescriptor$elementDescriptors$delegate$lambda(elementsCount, name, this));
}
protoOf(EnumDescriptor).ee = function () {
  return this.jj_1;
};
protoOf(EnumDescriptor).le = function (index) {
  return getChecked(_get_elementDescriptors__y23q9p(this), index);
};
protoOf(EnumDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (other == null)
    return false;
  if (!isInterface(other, SerialDescriptor))
    return false;
  if (!(other.ee() === ENUM_getInstance()))
    return false;
  if (!(this.td() === other.td()))
    return false;
  if (!equals(cachedSerialNames(this), cachedSerialNames(other)))
    return false;
  return true;
};
protoOf(EnumDescriptor).toString = function () {
  return joinToString(get_elementNames(this), ', ', this.td() + '(', ')');
};
protoOf(EnumDescriptor).hashCode = function () {
  var result = getStringHashCode(this.td());
  // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
  // Inline function 'kotlin.collections.fold' call
  var accumulator = 1;
  var _iterator__ex2g4s = get_elementNames(this).j();
  while (_iterator__ex2g4s.k()) {
    var element = _iterator__ex2g4s.l();
    var hash = accumulator;
    var tmp = imul(31, hash);
    // Inline function 'kotlin.hashCode' call
    var tmp1_elvis_lhs = element == null ? null : hashCode(element);
    accumulator = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
  }
  var elementsHashCode = accumulator;
  result = imul(31, result) + elementsHashCode | 0;
  return result;
};
function InlinePrimitiveDescriptor(name, primitiveSerializer) {
  return new InlineClassDescriptor(name, new InlinePrimitiveDescriptor$1(primitiveSerializer));
}
function InlineClassDescriptor(name, generatedSerializer) {
  PluginGeneratedSerialDescriptor.call(this, name, generatedSerializer, 1);
  this.zj_1 = true;
}
protoOf(InlineClassDescriptor).fe = function () {
  return this.zj_1;
};
protoOf(InlineClassDescriptor).hashCode = function () {
  return imul(protoOf(PluginGeneratedSerialDescriptor).hashCode.call(this), 31);
};
protoOf(InlineClassDescriptor).equals = function (other) {
  var tmp$ret$0;
  $l$block_5: {
    // Inline function 'kotlinx.serialization.internal.equalsImpl' call
    if (this === other) {
      tmp$ret$0 = true;
      break $l$block_5;
    }
    if (!(other instanceof InlineClassDescriptor)) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.td() === other.td())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(other.zj_1 && contentEquals(this.lj(), other.lj()))) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.ge() === other.ge())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.ge();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.le(index).td() === other.le(index).td())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.le(index).ee(), other.le(index).ee())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
      }
       while (inductionVariable < last);
    tmp$ret$0 = true;
  }
  return tmp$ret$0;
};
function InlinePrimitiveDescriptor$1($primitiveSerializer) {
  this.ak_1 = $primitiveSerializer;
}
protoOf(InlinePrimitiveDescriptor$1).bk = function () {
  // Inline function 'kotlin.arrayOf' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return [this.ak_1];
};
protoOf(InlinePrimitiveDescriptor$1).xc = function () {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
protoOf(InlinePrimitiveDescriptor$1).zc = function (encoder, value) {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
protoOf(InlinePrimitiveDescriptor$1).yc = function (decoder) {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
function missingFieldExceptionWithNewMessage(exception, message) {
  return exception.xd(message);
}
function jsonCachedSerialNames(_this__u8e3s4) {
  return cachedSerialNames(_this__u8e3s4);
}
function NullableSerializer(serializer) {
  this.dk_1 = serializer;
  this.ek_1 = new SerialDescriptorForNullable(this.dk_1.xc());
}
protoOf(NullableSerializer).xc = function () {
  return this.ek_1;
};
protoOf(NullableSerializer).fk = function (encoder, value) {
  if (!(value == null)) {
    encoder.ug();
    encoder.rg(this.dk_1, value);
  } else {
    encoder.gg();
  }
};
protoOf(NullableSerializer).zc = function (encoder, value) {
  return this.fk(encoder, value);
};
protoOf(NullableSerializer).yc = function (decoder) {
  return decoder.if() ? decoder.qf(this.dk_1) : decoder.jf();
};
protoOf(NullableSerializer).equals = function (other) {
  if (this === other)
    return true;
  if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
    return false;
  if (!(other instanceof NullableSerializer))
    THROW_CCE();
  if (!equals(this.dk_1, other.dk_1))
    return false;
  return true;
};
protoOf(NullableSerializer).hashCode = function () {
  return hashCode(this.dk_1);
};
function SerialDescriptorForNullable(original) {
  this.ae_1 = original;
  this.be_1 = this.ae_1.td() + '?';
  this.ce_1 = cachedSerialNames(this.ae_1);
}
protoOf(SerialDescriptorForNullable).td = function () {
  return this.be_1;
};
protoOf(SerialDescriptorForNullable).gf = function () {
  return this.ce_1;
};
protoOf(SerialDescriptorForNullable).yd = function () {
  return true;
};
protoOf(SerialDescriptorForNullable).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof SerialDescriptorForNullable))
    return false;
  if (!equals(this.ae_1, other.ae_1))
    return false;
  return true;
};
protoOf(SerialDescriptorForNullable).toString = function () {
  return toString(this.ae_1) + '?';
};
protoOf(SerialDescriptorForNullable).hashCode = function () {
  return imul(hashCode(this.ae_1), 31);
};
protoOf(SerialDescriptorForNullable).ee = function () {
  return this.ae_1.ee();
};
protoOf(SerialDescriptorForNullable).fe = function () {
  return this.ae_1.fe();
};
protoOf(SerialDescriptorForNullable).ge = function () {
  return this.ae_1.ge();
};
protoOf(SerialDescriptorForNullable).he = function () {
  return this.ae_1.he();
};
protoOf(SerialDescriptorForNullable).ie = function (index) {
  return this.ae_1.ie(index);
};
protoOf(SerialDescriptorForNullable).je = function (name) {
  return this.ae_1.je(name);
};
protoOf(SerialDescriptorForNullable).ke = function (index) {
  return this.ae_1.ke(index);
};
protoOf(SerialDescriptorForNullable).le = function (index) {
  return this.ae_1.le(index);
};
protoOf(SerialDescriptorForNullable).me = function (index) {
  return this.ae_1.me(index);
};
function get_EMPTY_DESCRIPTOR_ARRAY() {
  _init_properties_Platform_common_kt__3qzecs();
  return EMPTY_DESCRIPTOR_ARRAY;
}
var EMPTY_DESCRIPTOR_ARRAY;
function compactArray(_this__u8e3s4) {
  _init_properties_Platform_common_kt__3qzecs();
  // Inline function 'kotlin.takeUnless' call
  var tmp;
  // Inline function 'kotlin.collections.isNullOrEmpty' call
  if (!(_this__u8e3s4 == null || _this__u8e3s4.m())) {
    tmp = _this__u8e3s4;
  } else {
    tmp = null;
  }
  var tmp0_safe_receiver = tmp;
  var tmp_0;
  if (tmp0_safe_receiver == null) {
    tmp_0 = null;
  } else {
    // Inline function 'kotlin.collections.toTypedArray' call
    tmp_0 = copyToArray(tmp0_safe_receiver);
  }
  var tmp1_elvis_lhs = tmp_0;
  return tmp1_elvis_lhs == null ? get_EMPTY_DESCRIPTOR_ARRAY() : tmp1_elvis_lhs;
}
function cachedSerialNames(_this__u8e3s4) {
  _init_properties_Platform_common_kt__3qzecs();
  if (isInterface(_this__u8e3s4, CachedNames))
    return _this__u8e3s4.gf();
  var result = HashSet_init_$Create$_0(_this__u8e3s4.ge());
  var inductionVariable = 0;
  var last = _this__u8e3s4.ge();
  if (inductionVariable < last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.plusAssign' call
      var element = _this__u8e3s4.ie(i);
      result.i(element);
    }
     while (inductionVariable < last);
  return result;
}
var properties_initialized_Platform_common_kt_i7q4ty;
function _init_properties_Platform_common_kt__3qzecs() {
  if (!properties_initialized_Platform_common_kt_i7q4ty) {
    properties_initialized_Platform_common_kt_i7q4ty = true;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    EMPTY_DESCRIPTOR_ARRAY = [];
  }
}
function throwMissingFieldException(seen, goldenMask, descriptor) {
  // Inline function 'kotlin.collections.mutableListOf' call
  var missingFields = ArrayList_init_$Create$();
  var missingFieldsBits = goldenMask & ~seen;
  var inductionVariable = 0;
  if (inductionVariable < 32)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      if (!((missingFieldsBits & 1) === 0)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element = descriptor.ie(i);
        missingFields.i(element);
      }
      missingFieldsBits = missingFieldsBits >>> 1 | 0;
    }
     while (inductionVariable < 32);
  throw MissingFieldException_init_$Create$(missingFields, descriptor.td());
}
function hashCodeImpl(_this__u8e3s4, typeParams) {
  var result = getStringHashCode(_this__u8e3s4.td());
  result = imul(31, result) + contentHashCode(typeParams) | 0;
  var elementDescriptors = get_elementDescriptors(_this__u8e3s4);
  // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
  // Inline function 'kotlin.collections.fold' call
  var accumulator = 1;
  var _iterator__ex2g4s = elementDescriptors.j();
  while (_iterator__ex2g4s.k()) {
    var element = _iterator__ex2g4s.l();
    var hash = accumulator;
    var tmp = imul(31, hash);
    // Inline function 'kotlin.hashCode' call
    var tmp0_safe_receiver = element.td();
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : hashCode(tmp0_safe_receiver);
    accumulator = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
  }
  var namesHash = accumulator;
  // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
  // Inline function 'kotlin.collections.fold' call
  var accumulator_0 = 1;
  var _iterator__ex2g4s_0 = elementDescriptors.j();
  while (_iterator__ex2g4s_0.k()) {
    var element_0 = _iterator__ex2g4s_0.l();
    var hash_0 = accumulator_0;
    var tmp_0 = imul(31, hash_0);
    // Inline function 'kotlin.hashCode' call
    var tmp0_safe_receiver_0 = element_0.ee();
    var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode(tmp0_safe_receiver_0);
    accumulator_0 = tmp_0 + (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0) | 0;
  }
  var kindHash = accumulator_0;
  result = imul(31, result) + namesHash | 0;
  result = imul(31, result) + kindHash | 0;
  return result;
}
function toStringImpl(_this__u8e3s4) {
  var tmp = until(0, _this__u8e3s4.ge());
  var tmp_0 = _this__u8e3s4.td() + '(';
  return joinToString(tmp, ', ', tmp_0, ')', VOID, VOID, toStringImpl$lambda(_this__u8e3s4));
}
function _get_childSerializers__7vnyfa($this) {
  var tmp0 = $this.si_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('childSerializers', 1, tmp, PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca(), null);
  return tmp0.t();
}
function _get__hashCode__tgwhef_0($this) {
  var tmp0 = $this.ui_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz(), null);
  return tmp0.t();
}
function buildIndices($this) {
  var indices = HashMap_init_$Create$();
  var inductionVariable = 0;
  var last = $this.ni_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.set' call
      var key = $this.ni_1[i];
      indices.h2(key, i);
    }
     while (inductionVariable <= last);
  return indices;
}
function PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this$0) {
  return function () {
    var tmp0_safe_receiver = this$0.ki_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.bk();
    return tmp1_elvis_lhs == null ? get_EMPTY_SERIALIZER_ARRAY() : tmp1_elvis_lhs;
  };
}
function PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca() {
  return constructCallableReference(function (p0) {
    return _get_childSerializers__7vnyfa(p0);
  }, 1, 0, 4);
}
function PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this$0) {
  return function () {
    var tmp0_safe_receiver = this$0.ki_1;
    var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.ck();
    var tmp;
    if (tmp1_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList_init_$Create$_0(tmp1_safe_receiver.length);
      var inductionVariable = 0;
      var last = tmp1_safe_receiver.length;
      while (inductionVariable < last) {
        var item = tmp1_safe_receiver[inductionVariable];
        inductionVariable = inductionVariable + 1 | 0;
        var tmp$ret$2 = item.xc();
        destination.i(tmp$ret$2);
      }
      tmp = destination;
    }
    return compactArray(tmp);
  };
}
function PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka() {
  return constructCallableReference(function (p0) {
    return p0.lj();
  }, 1, 0, 5);
}
function PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this$0) {
  return function () {
    return hashCodeImpl(this$0, this$0.lj());
  };
}
function PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz() {
  return constructCallableReference(function (p0) {
    return _get__hashCode__tgwhef_0(p0);
  }, 1, 0, 6);
}
function PluginGeneratedSerialDescriptor(serialName, generatedSerializer, elementsCount) {
  generatedSerializer = generatedSerializer === VOID ? null : generatedSerializer;
  this.ji_1 = serialName;
  this.ki_1 = generatedSerializer;
  this.li_1 = elementsCount;
  this.mi_1 = -1;
  var tmp = this;
  var tmp_0 = 0;
  var tmp_1 = this.li_1;
  // Inline function 'kotlin.arrayOfNulls' call
  var tmp_2 = Array(tmp_1);
  while (tmp_0 < tmp_1) {
    tmp_2[tmp_0] = '[UNINITIALIZED]';
    tmp_0 = tmp_0 + 1 | 0;
  }
  tmp.ni_1 = tmp_2;
  var tmp_3 = this;
  // Inline function 'kotlin.arrayOfNulls' call
  var size = this.li_1;
  tmp_3.oi_1 = Array(size);
  this.pi_1 = null;
  this.qi_1 = booleanArray(this.li_1);
  this.ri_1 = emptyMap();
  var tmp_4 = this;
  var tmp_5 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_4.si_1 = lazy(tmp_5, PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this));
  var tmp_6 = this;
  var tmp_7 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_6.ti_1 = lazy(tmp_7, PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this));
  var tmp_8 = this;
  var tmp_9 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_8.ui_1 = lazy(tmp_9, PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this));
}
protoOf(PluginGeneratedSerialDescriptor).td = function () {
  return this.ji_1;
};
protoOf(PluginGeneratedSerialDescriptor).ge = function () {
  return this.li_1;
};
protoOf(PluginGeneratedSerialDescriptor).ee = function () {
  return CLASS_getInstance();
};
protoOf(PluginGeneratedSerialDescriptor).he = function () {
  var tmp0_elvis_lhs = this.pi_1;
  return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).gf = function () {
  return this.ri_1.p1();
};
protoOf(PluginGeneratedSerialDescriptor).lj = function () {
  var tmp0 = this.ti_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('typeParameterDescriptors', 1, tmp, PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka(), null);
  return tmp0.t();
};
protoOf(PluginGeneratedSerialDescriptor).mj = function (name, isOptional) {
  this.mi_1 = this.mi_1 + 1 | 0;
  this.ni_1[this.mi_1] = name;
  this.qi_1[this.mi_1] = isOptional;
  this.oi_1[this.mi_1] = null;
  if (this.mi_1 === (this.li_1 - 1 | 0)) {
    this.ri_1 = buildIndices(this);
  }
};
protoOf(PluginGeneratedSerialDescriptor).vi = function (name, isOptional, $super) {
  isOptional = isOptional === VOID ? false : isOptional;
  var tmp;
  if ($super === VOID) {
    this.mj(name, isOptional);
    tmp = Unit_instance;
  } else {
    tmp = $super.mj.call(this, name, isOptional);
  }
  return tmp;
};
protoOf(PluginGeneratedSerialDescriptor).le = function (index) {
  return getChecked(_get_childSerializers__7vnyfa(this), index).xc();
};
protoOf(PluginGeneratedSerialDescriptor).me = function (index) {
  return getChecked_0(this.qi_1, index);
};
protoOf(PluginGeneratedSerialDescriptor).ke = function (index) {
  var tmp0_elvis_lhs = getChecked(this.oi_1, index);
  return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).ie = function (index) {
  return getChecked(this.ni_1, index);
};
protoOf(PluginGeneratedSerialDescriptor).je = function (name) {
  var tmp0_elvis_lhs = this.ri_1.o1(name);
  return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).equals = function (other) {
  var tmp$ret$0;
  $l$block_5: {
    // Inline function 'kotlinx.serialization.internal.equalsImpl' call
    if (this === other) {
      tmp$ret$0 = true;
      break $l$block_5;
    }
    if (!(other instanceof PluginGeneratedSerialDescriptor)) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.td() === other.td())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!contentEquals(this.lj(), other.lj())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.ge() === other.ge())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.ge();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.le(index).td() === other.le(index).td())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.le(index).ee(), other.le(index).ee())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
      }
       while (inductionVariable < last);
    tmp$ret$0 = true;
  }
  return tmp$ret$0;
};
protoOf(PluginGeneratedSerialDescriptor).hashCode = function () {
  return _get__hashCode__tgwhef_0(this);
};
protoOf(PluginGeneratedSerialDescriptor).toString = function () {
  return toStringImpl(this);
};
function toStringImpl$lambda($this_toStringImpl) {
  return function (i) {
    return $this_toStringImpl.ie(i) + ': ' + $this_toStringImpl.le(i).td();
  };
}
function get_EMPTY_SERIALIZER_ARRAY() {
  _init_properties_PluginHelperInterfaces_kt__xgvzfp();
  return EMPTY_SERIALIZER_ARRAY;
}
var EMPTY_SERIALIZER_ARRAY;
function GeneratedSerializer() {
}
var properties_initialized_PluginHelperInterfaces_kt_ap8in1;
function _init_properties_PluginHelperInterfaces_kt__xgvzfp() {
  if (!properties_initialized_PluginHelperInterfaces_kt_ap8in1) {
    properties_initialized_PluginHelperInterfaces_kt_ap8in1 = true;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    EMPTY_SERIALIZER_ARRAY = [];
  }
}
function StringSerializer() {
  StringSerializer_instance = this;
  this.gk_1 = new PrimitiveSerialDescriptor('kotlin.String', STRING_getInstance());
}
protoOf(StringSerializer).xc = function () {
  return this.gk_1;
};
protoOf(StringSerializer).hk = function (encoder, value) {
  return encoder.kg(value);
};
protoOf(StringSerializer).zc = function (encoder, value) {
  return this.hk(encoder, (!(value == null) ? typeof value === 'string' : false) ? value : THROW_CCE());
};
protoOf(StringSerializer).yc = function (decoder) {
  return decoder.nf();
};
var StringSerializer_instance;
function StringSerializer_getInstance() {
  if (StringSerializer_instance == null)
    new StringSerializer();
  return StringSerializer_instance;
}
function error($this) {
  throw IllegalStateException_init_$Create$('Primitive descriptor ' + $this.ik_1 + ' does not have elements');
}
function PrimitiveSerialDescriptor(serialName, kind) {
  this.ik_1 = serialName;
  this.jk_1 = kind;
}
protoOf(PrimitiveSerialDescriptor).td = function () {
  return this.ik_1;
};
protoOf(PrimitiveSerialDescriptor).ee = function () {
  return this.jk_1;
};
protoOf(PrimitiveSerialDescriptor).ge = function () {
  return 0;
};
protoOf(PrimitiveSerialDescriptor).ie = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).je = function (name) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).me = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).le = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).ke = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).toString = function () {
  return 'PrimitiveDescriptor(' + this.ik_1 + ')';
};
protoOf(PrimitiveSerialDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof PrimitiveSerialDescriptor))
    return false;
  if (this.ik_1 === other.ik_1 && equals(this.jk_1, other.jk_1))
    return true;
  return false;
};
protoOf(PrimitiveSerialDescriptor).hashCode = function () {
  return getStringHashCode(this.ik_1) + imul(31, this.jk_1.hashCode()) | 0;
};
function IntSerializer() {
  IntSerializer_instance = this;
  this.kk_1 = new PrimitiveSerialDescriptor('kotlin.Int', INT_getInstance());
}
protoOf(IntSerializer).xc = function () {
  return this.kk_1;
};
protoOf(IntSerializer).lk = function (encoder, value) {
  return encoder.hg(value);
};
protoOf(IntSerializer).zc = function (encoder, value) {
  return this.lk(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
};
protoOf(IntSerializer).yc = function (decoder) {
  return decoder.kf();
};
var IntSerializer_instance;
function IntSerializer_getInstance() {
  if (IntSerializer_instance == null)
    new IntSerializer();
  return IntSerializer_instance;
}
function LongSerializer() {
  LongSerializer_instance = this;
  this.mk_1 = new PrimitiveSerialDescriptor('kotlin.Long', LONG_getInstance());
}
protoOf(LongSerializer).xc = function () {
  return this.mk_1;
};
protoOf(LongSerializer).nk = function (encoder, value) {
  return encoder.ig(value);
};
protoOf(LongSerializer).zc = function (encoder, value) {
  return this.nk(encoder, value instanceof Long ? value : THROW_CCE());
};
protoOf(LongSerializer).yc = function (decoder) {
  return decoder.lf();
};
var LongSerializer_instance;
function LongSerializer_getInstance() {
  if (LongSerializer_instance == null)
    new LongSerializer();
  return LongSerializer_instance;
}
function DoubleSerializer() {
  DoubleSerializer_instance = this;
  this.ok_1 = new PrimitiveSerialDescriptor('kotlin.Double', DOUBLE_getInstance());
}
protoOf(DoubleSerializer).xc = function () {
  return this.ok_1;
};
protoOf(DoubleSerializer).pk = function (encoder, value) {
  return encoder.jg(value);
};
protoOf(DoubleSerializer).zc = function (encoder, value) {
  return this.pk(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
};
protoOf(DoubleSerializer).yc = function (decoder) {
  return decoder.mf();
};
var DoubleSerializer_instance;
function DoubleSerializer_getInstance() {
  if (DoubleSerializer_instance == null)
    new DoubleSerializer();
  return DoubleSerializer_instance;
}
function NamedValueDecoder() {
  TaggedDecoder.call(this);
}
protoOf(NamedValueDecoder).sk = function (_this__u8e3s4, index) {
  return this.uk(this.tk(_this__u8e3s4, index));
};
protoOf(NamedValueDecoder).uk = function (nestedName) {
  var tmp0_elvis_lhs = this.xk();
  return this.yk(tmp0_elvis_lhs == null ? '' : tmp0_elvis_lhs, nestedName);
};
protoOf(NamedValueDecoder).tk = function (descriptor, index) {
  return descriptor.ie(index);
};
protoOf(NamedValueDecoder).yk = function (parentName, childName) {
  var tmp;
  // Inline function 'kotlin.text.isEmpty' call
  if (charSequenceLength(parentName) === 0) {
    tmp = childName;
  } else {
    tmp = parentName + '.' + childName;
  }
  return tmp;
};
protoOf(NamedValueDecoder).zk = function () {
  return this.vk_1.m() ? '$' : joinToString(this.vk_1, '.', '$.');
};
function tagBlock($this, tag, block) {
  $this.hl(tag);
  var r = block();
  if (!$this.wk_1) {
    $this.il();
  }
  $this.wk_1 = false;
  return r;
}
function TaggedDecoder$decodeSerializableElement$lambda(this$0, $deserializer, $previousValue) {
  return function () {
    return this$0.pf($deserializer, $previousValue);
  };
}
function TaggedDecoder$decodeNullableSerializableElement$lambda(this$0, $deserializer, $previousValue) {
  return function () {
    var tmp0 = this$0;
    // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
    var isNullabilitySupported = $deserializer.xc().yd();
    var tmp;
    if (isNullabilitySupported || tmp0.if()) {
      tmp = this$0.pf($deserializer, $previousValue);
    } else {
      tmp = tmp0.jf();
    }
    return tmp;
  };
}
function TaggedDecoder() {
  var tmp = this;
  // Inline function 'kotlin.collections.arrayListOf' call
  tmp.vk_1 = ArrayList_init_$Create$();
  this.wk_1 = false;
}
protoOf(TaggedDecoder).ag = function () {
  return EmptySerializersModule_0();
};
protoOf(TaggedDecoder).al = function (tag) {
  throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
};
protoOf(TaggedDecoder).bl = function (tag) {
  return true;
};
protoOf(TaggedDecoder).cl = function (tag) {
  var tmp = this.al(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).dl = function (tag) {
  var tmp = this.al(tag);
  return tmp instanceof Long ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).el = function (tag) {
  var tmp = this.al(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).fl = function (tag) {
  var tmp = this.al(tag);
  return typeof tmp === 'string' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).gl = function (tag, enumDescriptor) {
  var tmp = this.al(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).pf = function (deserializer, previousValue) {
  return this.qf(deserializer);
};
protoOf(TaggedDecoder).if = function () {
  var tmp0_elvis_lhs = this.xk();
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return false;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var currentTag = tmp;
  return this.bl(currentTag);
};
protoOf(TaggedDecoder).jf = function () {
  return null;
};
protoOf(TaggedDecoder).kf = function () {
  return this.cl(this.il());
};
protoOf(TaggedDecoder).lf = function () {
  return this.dl(this.il());
};
protoOf(TaggedDecoder).mf = function () {
  return this.el(this.il());
};
protoOf(TaggedDecoder).nf = function () {
  return this.fl(this.il());
};
protoOf(TaggedDecoder).of = function (enumDescriptor) {
  return this.gl(this.il(), enumDescriptor);
};
protoOf(TaggedDecoder).rf = function (descriptor) {
  return this;
};
protoOf(TaggedDecoder).sf = function (descriptor) {
};
protoOf(TaggedDecoder).tf = function (descriptor, index) {
  return this.cl(this.sk(descriptor, index));
};
protoOf(TaggedDecoder).uf = function (descriptor, index) {
  return this.dl(this.sk(descriptor, index));
};
protoOf(TaggedDecoder).vf = function (descriptor, index) {
  return this.el(this.sk(descriptor, index));
};
protoOf(TaggedDecoder).wf = function (descriptor, index) {
  return this.fl(this.sk(descriptor, index));
};
protoOf(TaggedDecoder).xf = function (descriptor, index, deserializer, previousValue) {
  var tmp = this.sk(descriptor, index);
  return tagBlock(this, tmp, TaggedDecoder$decodeSerializableElement$lambda(this, deserializer, previousValue));
};
protoOf(TaggedDecoder).zf = function (descriptor, index, deserializer, previousValue) {
  var tmp = this.sk(descriptor, index);
  return tagBlock(this, tmp, TaggedDecoder$decodeNullableSerializableElement$lambda(this, deserializer, previousValue));
};
protoOf(TaggedDecoder).xk = function () {
  return lastOrNull(this.vk_1);
};
protoOf(TaggedDecoder).hl = function (name) {
  this.vk_1.i(name);
};
protoOf(TaggedDecoder).il = function () {
  var r = this.vk_1.r2(get_lastIndex_0(this.vk_1));
  this.wk_1 = true;
  return r;
};
function get_EmptySerializersModuleLegacyJs() {
  _init_properties_SerializersModule_kt__u78ha3();
  return EmptySerializersModule;
}
var EmptySerializersModule;
function SerializersModule() {
}
protoOf(SerializersModule).zd = function (kClass, typeArgumentsSerializers, $super) {
  typeArgumentsSerializers = typeArgumentsSerializers === VOID ? emptyList() : typeArgumentsSerializers;
  return $super === VOID ? this.jl(kClass, typeArgumentsSerializers) : $super.jl.call(this, kClass, typeArgumentsSerializers);
};
function SerialModuleImpl(class2ContextualFactory, polyBase2Serializers, polyBase2DefaultSerializerProvider, polyBase2NamedSerializers, polyBase2DefaultDeserializerProvider, hasInterfaceContextualSerializers) {
  SerializersModule.call(this);
  this.kl_1 = class2ContextualFactory;
  this.ll_1 = polyBase2Serializers;
  this.ml_1 = polyBase2DefaultSerializerProvider;
  this.nl_1 = polyBase2NamedSerializers;
  this.ol_1 = polyBase2DefaultDeserializerProvider;
  this.pl_1 = hasInterfaceContextualSerializers;
}
protoOf(SerialModuleImpl).yg = function (baseClass, value) {
  if (!baseClass.y7(value))
    return null;
  var tmp0_safe_receiver = this.ll_1.o1(baseClass);
  var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.o1(getKClassFromExpression(value));
  var registered = (!(tmp == null) ? isInterface(tmp, SerializationStrategy) : false) ? tmp : null;
  if (!(registered == null))
    return registered;
  var tmp_0 = this.ml_1.o1(baseClass);
  var tmp1_safe_receiver = (!(tmp_0 == null) ? typeof tmp_0 === 'function' : false) ? tmp_0 : null;
  return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(value);
};
protoOf(SerialModuleImpl).xg = function (baseClass, serializedClassName) {
  var tmp0_safe_receiver = this.nl_1.o1(baseClass);
  var tmp;
  if (tmp0_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlin.collections.get' call
    tmp = (isInterface(tmp0_safe_receiver, KtMap) ? tmp0_safe_receiver : THROW_CCE()).o1(serializedClassName);
  }
  var tmp_0 = tmp;
  var registered = (!(tmp_0 == null) ? isInterface(tmp_0, KSerializer) : false) ? tmp_0 : null;
  if (!(registered == null))
    return registered;
  var tmp_1 = this.ol_1.o1(baseClass);
  var tmp1_safe_receiver = (!(tmp_1 == null) ? typeof tmp_1 === 'function' : false) ? tmp_1 : null;
  return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(serializedClassName);
};
protoOf(SerialModuleImpl).jl = function (kClass, typeArgumentsSerializers) {
  var tmp0_safe_receiver = this.kl_1.o1(kClass);
  var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.ql(typeArgumentsSerializers);
  return (tmp == null ? true : isInterface(tmp, KSerializer)) ? tmp : null;
};
var properties_initialized_SerializersModule_kt_fjigjn;
function _init_properties_SerializersModule_kt__u78ha3() {
  if (!properties_initialized_SerializersModule_kt_fjigjn) {
    properties_initialized_SerializersModule_kt_fjigjn = true;
    EmptySerializersModule = new SerialModuleImpl(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), false);
  }
}
function EmptySerializersModule_0() {
  return get_EmptySerializersModuleLegacyJs();
}
function SerializableWith() {
}
function getChecked(_this__u8e3s4, index) {
  if (!(0 <= index ? index <= (_this__u8e3s4.length - 1 | 0) : false))
    throw IndexOutOfBoundsException_init_$Create$('Index ' + index + ' out of bounds ' + get_indices(_this__u8e3s4).toString());
  return _this__u8e3s4[index];
}
function getChecked_0(_this__u8e3s4, index) {
  if (!(0 <= index ? index <= (_this__u8e3s4.length - 1 | 0) : false))
    throw IndexOutOfBoundsException_init_$Create$('Index ' + index + ' out of bounds ' + get_indices_0(_this__u8e3s4).toString());
  return _this__u8e3s4[index];
}
//region block: post-declaration
protoOf(SerialDescriptorImpl).yd = get_isNullable;
protoOf(SerialDescriptorImpl).fe = get_isInline;
protoOf(AbstractDecoder).yf = decodeSerializableElement$default;
protoOf(AbstractDecoder).qf = decodeSerializableValue;
protoOf(AbstractDecoder).bg = decodeSequentially;
protoOf(AbstractDecoder).dg = decodeCollectionSize;
protoOf(AbstractEncoder).ug = encodeNotNullMark;
protoOf(AbstractEncoder).vg = beginCollection;
protoOf(AbstractEncoder).rg = encodeSerializableValue;
protoOf(AbstractEncoder).tg = encodeNullableSerializableValue;
protoOf(AbstractEncoder).wg = shouldEncodeElementDefault;
protoOf(ListLikeDescriptor).yd = get_isNullable;
protoOf(ListLikeDescriptor).fe = get_isInline;
protoOf(ListLikeDescriptor).he = get_annotations;
protoOf(PluginGeneratedSerialDescriptor).yd = get_isNullable;
protoOf(PluginGeneratedSerialDescriptor).fe = get_isInline;
protoOf(InlinePrimitiveDescriptor$1).ck = typeParametersSerializers;
protoOf(PrimitiveSerialDescriptor).yd = get_isNullable;
protoOf(PrimitiveSerialDescriptor).fe = get_isInline;
protoOf(PrimitiveSerialDescriptor).he = get_annotations;
protoOf(TaggedDecoder).yf = decodeSerializableElement$default;
protoOf(TaggedDecoder).qf = decodeSerializableValue;
protoOf(TaggedDecoder).bg = decodeSequentially;
protoOf(TaggedDecoder).dg = decodeCollectionSize;
//endregion
//region block: exports
export {
  CONTEXTUAL_getInstance as CONTEXTUAL_getInstance20l1ap7hne5we,
  ENUM_getInstance as ENUM_getInstance25239ht7oag6o,
  CLASS_getInstance as CLASS_getInstance3jy3ydw5832xx,
  LIST_getInstance as LIST_getInstance2vniy0f7mmb29,
  MAP_getInstance as MAP_getInstance3e78m6vf98i66,
  OBJECT_getInstance as OBJECT_getInstancerq52wx6ymebx,
  DoubleSerializer_getInstance as DoubleSerializer_getInstance1tuvibnyg07o1,
  IntSerializer_getInstance as IntSerializer_getInstance3jyhns9ue706g,
  LongSerializer_getInstance as LongSerializer_getInstancepwtz4tlewrfw,
  StringSerializer_getInstance as StringSerializer_getInstance1p9zda6r6kb23,
  SealedClassSerializer_init_$Create$ as SealedClassSerializer_init_$Create$15ycao7509ndt,
  SerializationException_init_$Create$_0 as SerializationException_init_$Create$1oc2fm69kd4pj,
  SerializationException_init_$Init$_1 as SerializationException_init_$Init$1mber2alabcfv,
  UnknownFieldException_init_$Create$ as UnknownFieldException_init_$Create$3n7tjepqfwwvc,
  get_nullable as get_nullable197rfua9r7fsz,
  serializer as serializer1x79l67jvwntn,
  PolymorphicKind as PolymorphicKindla9gurooefwb,
  PrimitiveKind as PrimitiveKindndgbuh6is7ze,
  SerialDescriptor as SerialDescriptor2pelqekb5ic3a,
  ENUM as ENUMlmq49cvwy4ow,
  getContextualDescriptor as getContextualDescriptor2n1gf3b895yb8,
  AbstractDecoder as AbstractDecoder35guh02ubh2hm,
  AbstractEncoder as AbstractEncoder2gxtu3xmy3f8j,
  CompositeDecoder as CompositeDecoder2tzm7wpwkr0og,
  CompositeEncoder as CompositeEncoderknecpkexzn3v,
  Decoder as Decoder23nde051s631g,
  Encoder as Encoderqvmrpqtq8hnu,
  AbstractPolymorphicSerializer as AbstractPolymorphicSerializer1ccxwp48nfy58,
  ArrayListSerializer as ArrayListSerializer7k5wnrulb3y6,
  ElementMarker as ElementMarker33ojvsajwmzts,
  typeParametersSerializers as typeParametersSerializers2likxjr48tr7y,
  GeneratedSerializer as GeneratedSerializer1f7t7hssdd2ws,
  InlinePrimitiveDescriptor as InlinePrimitiveDescriptor3i6ccn1a4fw94,
  NamedValueDecoder as NamedValueDecoderzk26ztf92xbq,
  PluginGeneratedSerialDescriptor as PluginGeneratedSerialDescriptorqdzeg5asqhfg,
  createSimpleEnumSerializer as createSimpleEnumSerializer2guioz11kk1m0,
  jsonCachedSerialNames as jsonCachedSerialNameslxufy2gu43jt,
  missingFieldExceptionWithNewMessage as missingFieldExceptionWithNewMessage2gqddjx5jxge9,
  throwMissingFieldException as throwMissingFieldException2cmke0v3ynf14,
  EmptySerializersModule_0 as EmptySerializersModule991ju6pz9b79,
  DeserializationStrategy as DeserializationStrategy1z3z5pj9f7zc8,
  MissingFieldException as MissingFieldException24tqif29emcmi,
  SerializableWith as SerializableWithd2dap36updxd,
  SerializationException as SerializationExceptioneqrdve3ts2n9,
  SerializationStrategy as SerializationStrategyh6ouydnm6hci,
};
//endregion

