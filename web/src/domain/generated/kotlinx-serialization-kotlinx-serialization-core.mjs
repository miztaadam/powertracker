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
  return deserializer.lc(this);
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
  return $super === VOID ? this.kf(descriptor, index, deserializer, previousValue) : $super.kf.call(this, descriptor, index, deserializer, previousValue);
}
initMetadataForInterface(CompositeDecoder, 'CompositeDecoder');
initMetadataForClass(AbstractDecoder, 'AbstractDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
function encodeNotNullMark() {
}
function beginCollection(descriptor, collectionSize) {
  return this.ef(descriptor);
}
function encodeSerializableValue(serializer, value) {
  serializer.mc(this, value);
}
function encodeNullableSerializableValue(serializer, value) {
  var isNullabilitySupported = serializer.kc().ld();
  if (isNullabilitySupported) {
    return this.eg(isInterface(serializer, SerializationStrategy) ? serializer : THROW_CCE(), value);
  }
  if (value == null) {
    this.tf();
  } else {
    this.hg();
    this.eg(serializer, value);
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
  var tmp0_elvis_lhs = _this__u8e3s4.nc(encoder, value);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    throwSubtypeNotRegistered(getKClassFromExpression(value), _this__u8e3s4.oc());
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function findPolymorphicSerializer_0(_this__u8e3s4, decoder, klassName) {
  var tmp0_elvis_lhs = _this__u8e3s4.pc(decoder, klassName);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    throwSubtypeNotRegistered_0(klassName, _this__u8e3s4.oc());
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function SealedClassSerializer_init_$Init$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations, $this) {
  SealedClassSerializer.call($this, serialName, baseClass, subclasses, subclassSerializers);
  $this.rc_1 = asList(classAnnotations);
  return $this;
}
function SealedClassSerializer_init_$Create$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations) {
  return SealedClassSerializer_init_$Init$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations, objectCreate(protoOf(SealedClassSerializer)));
}
function SealedClassSerializer$descriptor$delegate$lambda$lambda$lambda(this$0) {
  return function ($this$buildSerialDescriptor) {
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = this$0.uc_1.r().j();
    while (_iterator__ex2g4s.k()) {
      var element = _iterator__ex2g4s.l();
      // Inline function 'kotlin.collections.component1' call
      var name = element.s();
      // Inline function 'kotlin.collections.component2' call
      var serializer = element.t();
      $this$buildSerialDescriptor.dd(name, serializer.kc());
    }
    return Unit_instance;
  };
}
function SealedClassSerializer$descriptor$delegate$lambda$lambda(this$0) {
  return function ($this$buildSerialDescriptor) {
    $this$buildSerialDescriptor.dd('type', serializer(StringCompanionObject_instance).kc());
    var tmp = 'kotlinx.serialization.Sealed<' + this$0.qc_1.j7() + '>';
    var tmp_0 = CONTEXTUAL_getInstance();
    var elementDescriptor = buildSerialDescriptor(tmp, tmp_0, [], SealedClassSerializer$descriptor$delegate$lambda$lambda$lambda(this$0));
    $this$buildSerialDescriptor.dd('value', elementDescriptor);
    $this$buildSerialDescriptor.xc_1 = this$0.rc_1;
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
    return p0.kc();
  }, 1, 0, 0);
}
function SealedClassSerializer$$inlined$groupingBy$1($this) {
  this.ed_1 = $this;
}
protoOf(SealedClassSerializer$$inlined$groupingBy$1).t1 = function () {
  return this.ed_1.j();
};
protoOf(SealedClassSerializer$$inlined$groupingBy$1).fd = function (element) {
  return element.t().kc().gd();
};
protoOf(SealedClassSerializer$$inlined$groupingBy$1).u1 = function (element) {
  return this.fd(element);
};
function SealedClassSerializer(serialName, baseClass, subclasses, subclassSerializers) {
  AbstractPolymorphicSerializer.call(this);
  this.qc_1 = baseClass;
  this.rc_1 = emptyList();
  var tmp = this;
  var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp.sc_1 = lazy(tmp_0, SealedClassSerializer$descriptor$delegate$lambda(serialName, this));
  if (!(subclasses.length === subclassSerializers.length)) {
    throw IllegalArgumentException_init_$Create$('All subclasses of sealed class ' + this.qc_1.j7() + ' should be marked @Serializable');
  }
  this.tc_1 = toMap(zip(subclasses, subclassSerializers));
  var tmp_1 = this;
  // Inline function 'kotlin.collections.groupingBy' call
  var this_0 = this.tc_1.r();
  // Inline function 'kotlin.collections.aggregate' call
  var tmp0 = new SealedClassSerializer$$inlined$groupingBy$1(this_0);
  // Inline function 'kotlin.collections.mutableMapOf' call
  // Inline function 'kotlin.collections.aggregateTo' call
  var destination = LinkedHashMap_init_$Create$();
  // Inline function 'kotlin.collections.iterator' call
  var _iterator__ex2g4s = tmp0.t1();
  while (_iterator__ex2g4s.k()) {
    var e = _iterator__ex2g4s.l();
    var key = tmp0.u1(e);
    var accumulator = destination.c1(key);
    accumulator == null && !destination.a1(key);
    if (!(accumulator == null)) {
      // Inline function 'kotlin.error' call
      var message = "Multiple sealed subclasses of '" + toString(this.qc_1) + "' have the same serial name '" + key + "':" + (" '" + toString(accumulator.s()) + "', '" + toString(e.s()) + "'");
      throw IllegalStateException_init_$Create$(toString(message));
    }
    // Inline function 'kotlin.collections.set' call
    destination.v1(key, e);
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
    destination_0.v1(tmp_2, tmp$ret$12);
  }
  tmp_1.uc_1 = destination_0;
}
protoOf(SealedClassSerializer).oc = function () {
  return this.qc_1;
};
protoOf(SealedClassSerializer).kc = function () {
  var tmp0 = this.sc_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('descriptor', 1, tmp, SealedClassSerializer$_get_descriptor_$ref_m511rz(), null);
  return tmp0.t();
};
protoOf(SealedClassSerializer).pc = function (decoder, klassName) {
  // Inline function 'kotlin.collections.get' call
  var this_0 = this.uc_1;
  var tmp0_elvis_lhs = (isInterface(this_0, KtMap) ? this_0 : THROW_CCE()).c1(klassName);
  return tmp0_elvis_lhs == null ? protoOf(AbstractPolymorphicSerializer).pc.call(this, decoder, klassName) : tmp0_elvis_lhs;
};
protoOf(SealedClassSerializer).nc = function (encoder, value) {
  var tmp0_elvis_lhs = this.tc_1.c1(getKClassFromExpression(value));
  var tmp1_safe_receiver = tmp0_elvis_lhs == null ? protoOf(AbstractPolymorphicSerializer).nc.call(this, encoder, value) : tmp0_elvis_lhs;
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
  this.id_1 = missingFields;
  this.jd_1 = serialName;
}
protoOf(MissingFieldException).kd = function (newMessage) {
  return new MissingFieldException(newMessage, this, this.id_1, this.jd_1);
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
  if (_this__u8e3s4.kc().ld()) {
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
    var tmp0_safe_receiver_0 = _this__u8e3s4.md(tmp0_safe_receiver);
    tmp = tmp0_safe_receiver_0 == null ? null : tmp0_safe_receiver_0.kc();
  }
  return tmp;
}
function get_capturedKClass(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4 instanceof ContextDescriptor) {
    tmp = _this__u8e3s4.qd_1;
  } else {
    if (_this__u8e3s4 instanceof SerialDescriptorForNullable) {
      tmp = get_capturedKClass(_this__u8e3s4.nd_1);
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
  this.be_1 = $this_elementDescriptors;
  this.ae_1 = $this_elementDescriptors.td();
}
protoOf(elementDescriptors$1).k = function () {
  return this.ae_1 > 0;
};
protoOf(elementDescriptors$1).l = function () {
  var tmp = this.be_1.td();
  var _unary__edvuaz = this.ae_1;
  this.ae_1 = _unary__edvuaz - 1 | 0;
  return this.be_1.yd(tmp - _unary__edvuaz | 0);
};
function elementDescriptors$$inlined$Iterable$1($this_elementDescriptors) {
  this.ce_1 = $this_elementDescriptors;
}
protoOf(elementDescriptors$$inlined$Iterable$1).j = function () {
  return new elementDescriptors$1(this.ce_1);
};
function elementNames$1($this_elementNames) {
  this.ee_1 = $this_elementNames;
  this.de_1 = $this_elementNames.td();
}
protoOf(elementNames$1).k = function () {
  return this.de_1 > 0;
};
protoOf(elementNames$1).l = function () {
  var tmp = this.ee_1.td();
  var _unary__edvuaz = this.de_1;
  this.de_1 = _unary__edvuaz - 1 | 0;
  return this.ee_1.vd(tmp - _unary__edvuaz | 0);
};
function elementNames$$inlined$Iterable$1($this_elementNames) {
  this.fe_1 = $this_elementNames;
}
protoOf(elementNames$$inlined$Iterable$1).j = function () {
  return new elementNames$1(this.fe_1);
};
function ClassSerialDescriptorBuilder(serialName) {
  this.vc_1 = serialName;
  this.wc_1 = false;
  this.xc_1 = emptyList();
  this.yc_1 = ArrayList_init_$Create$();
  this.zc_1 = HashSet_init_$Create$();
  this.ad_1 = ArrayList_init_$Create$();
  this.bd_1 = ArrayList_init_$Create$();
  this.cd_1 = ArrayList_init_$Create$();
}
protoOf(ClassSerialDescriptorBuilder).ge = function (elementName, descriptor, annotations, isOptional) {
  // Inline function 'kotlin.require' call
  if (!this.zc_1.i(elementName)) {
    var message = "Element with name '" + elementName + "' is already registered in " + this.vc_1;
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  // Inline function 'kotlin.collections.plusAssign' call
  this.yc_1.i(elementName);
  // Inline function 'kotlin.collections.plusAssign' call
  this.ad_1.i(descriptor);
  // Inline function 'kotlin.collections.plusAssign' call
  this.bd_1.i(annotations);
  // Inline function 'kotlin.collections.plusAssign' call
  this.cd_1.i(isOptional);
};
protoOf(ClassSerialDescriptorBuilder).dd = function (elementName, descriptor, annotations, isOptional, $super) {
  annotations = annotations === VOID ? emptyList() : annotations;
  isOptional = isOptional === VOID ? false : isOptional;
  var tmp;
  if ($super === VOID) {
    this.ge(elementName, descriptor, annotations, isOptional);
    tmp = Unit_instance;
  } else {
    tmp = $super.ge.call(this, elementName, descriptor, annotations, isOptional);
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
  return new SerialDescriptorImpl(serialName, kind, sdBuilder.yc_1.o(), toList(typeParameters), sdBuilder);
}
function _get__hashCode__tgwhef($this) {
  var tmp0 = $this.se_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp(), null);
  return tmp0.t();
}
function SerialDescriptorImpl$_hashCode$delegate$lambda(this$0) {
  return function () {
    return hashCodeImpl(this$0, this$0.re_1);
  };
}
function SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp() {
  return constructCallableReference(function (p0) {
    return _get__hashCode__tgwhef(p0);
  }, 1, 0, 1);
}
function SerialDescriptorImpl(serialName, kind, elementsCount, typeParameters, builder) {
  this.he_1 = serialName;
  this.ie_1 = kind;
  this.je_1 = elementsCount;
  this.ke_1 = builder.xc_1;
  this.le_1 = toHashSet(builder.yc_1);
  var tmp = this;
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = builder.yc_1;
  tmp.me_1 = copyToArray(this_0);
  this.ne_1 = compactArray(builder.ad_1);
  var tmp_0 = this;
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_1 = builder.bd_1;
  tmp_0.oe_1 = copyToArray(this_1);
  this.pe_1 = toBooleanArray(builder.cd_1);
  var tmp_1 = this;
  // Inline function 'kotlin.collections.map' call
  var this_2 = withIndex(this.me_1);
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$_0(collectionSizeOrDefault(this_2, 10));
  var _iterator__ex2g4s = this_2.j();
  while (_iterator__ex2g4s.k()) {
    var item = _iterator__ex2g4s.l();
    var tmp$ret$4 = to(item.o9_1, item.n9_1);
    destination.i(tmp$ret$4);
  }
  tmp_1.qe_1 = toMap(destination);
  this.re_1 = compactArray(typeParameters);
  var tmp_2 = this;
  tmp_2.se_1 = lazy_0(SerialDescriptorImpl$_hashCode$delegate$lambda(this));
}
protoOf(SerialDescriptorImpl).gd = function () {
  return this.he_1;
};
protoOf(SerialDescriptorImpl).rd = function () {
  return this.ie_1;
};
protoOf(SerialDescriptorImpl).td = function () {
  return this.je_1;
};
protoOf(SerialDescriptorImpl).ud = function () {
  return this.ke_1;
};
protoOf(SerialDescriptorImpl).te = function () {
  return this.le_1;
};
protoOf(SerialDescriptorImpl).vd = function (index) {
  return getChecked(this.me_1, index);
};
protoOf(SerialDescriptorImpl).wd = function (name) {
  var tmp0_elvis_lhs = this.qe_1.c1(name);
  return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
};
protoOf(SerialDescriptorImpl).xd = function (index) {
  return getChecked(this.oe_1, index);
};
protoOf(SerialDescriptorImpl).yd = function (index) {
  return getChecked(this.ne_1, index);
};
protoOf(SerialDescriptorImpl).zd = function (index) {
  return getChecked_0(this.pe_1, index);
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
    if (!(this.gd() === other.gd())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!contentEquals(this.re_1, other.re_1)) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.td() === other.td())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.td();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.yd(index).gd() === other.yd(index).gd())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.yd(index).rd(), other.yd(index).rd())) {
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
  return ensureNotNull(getKClassFromExpression(this).j7());
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
protoOf(AbstractDecoder).ue = function () {
  throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
};
protoOf(AbstractDecoder).ve = function () {
  return true;
};
protoOf(AbstractDecoder).we = function () {
  return null;
};
protoOf(AbstractDecoder).xe = function () {
  var tmp = this.ue();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).ye = function () {
  var tmp = this.ue();
  return tmp instanceof Long ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).ze = function () {
  var tmp = this.ue();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).af = function () {
  var tmp = this.ue();
  return typeof tmp === 'string' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).bf = function (enumDescriptor) {
  var tmp = this.ue();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).cf = function (deserializer, previousValue) {
  return this.df(deserializer);
};
protoOf(AbstractDecoder).ef = function (descriptor) {
  return this;
};
protoOf(AbstractDecoder).ff = function (descriptor) {
};
protoOf(AbstractDecoder).gf = function (descriptor, index) {
  return this.xe();
};
protoOf(AbstractDecoder).hf = function (descriptor, index) {
  return this.ye();
};
protoOf(AbstractDecoder).if = function (descriptor, index) {
  return this.ze();
};
protoOf(AbstractDecoder).jf = function (descriptor, index) {
  return this.af();
};
protoOf(AbstractDecoder).kf = function (descriptor, index, deserializer, previousValue) {
  return this.cf(deserializer, previousValue);
};
protoOf(AbstractDecoder).mf = function (descriptor, index, deserializer, previousValue) {
  // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
  var isNullabilitySupported = deserializer.kc().ld();
  var tmp;
  if (isNullabilitySupported || this.ve()) {
    tmp = this.cf(deserializer, previousValue);
  } else {
    tmp = this.we();
  }
  return tmp;
};
function AbstractEncoder() {
}
protoOf(AbstractEncoder).ef = function (descriptor) {
  return this;
};
protoOf(AbstractEncoder).ff = function (descriptor) {
};
protoOf(AbstractEncoder).rf = function (descriptor, index) {
  return true;
};
protoOf(AbstractEncoder).sf = function (value) {
  throw SerializationException_init_$Create$_0('Non-serializable ' + toString(getKClassFromExpression(value)) + ' is not supported by ' + toString(getKClassFromExpression(this)) + ' encoder');
};
protoOf(AbstractEncoder).tf = function () {
  throw SerializationException_init_$Create$_0("'null' is not supported by default");
};
protoOf(AbstractEncoder).uf = function (value) {
  return this.sf(value);
};
protoOf(AbstractEncoder).vf = function (value) {
  return this.sf(value);
};
protoOf(AbstractEncoder).wf = function (value) {
  return this.sf(value);
};
protoOf(AbstractEncoder).xf = function (value) {
  return this.sf(value);
};
protoOf(AbstractEncoder).yf = function (enumDescriptor, index) {
  return this.sf(index);
};
protoOf(AbstractEncoder).zf = function (descriptor, index, value) {
  if (this.rf(descriptor, index)) {
    this.uf(value);
  }
};
protoOf(AbstractEncoder).ag = function (descriptor, index, value) {
  if (this.rf(descriptor, index)) {
    this.vf(value);
  }
};
protoOf(AbstractEncoder).bg = function (descriptor, index, value) {
  if (this.rf(descriptor, index)) {
    this.wf(value);
  }
};
protoOf(AbstractEncoder).cg = function (descriptor, index, value) {
  if (this.rf(descriptor, index)) {
    this.xf(value);
  }
};
protoOf(AbstractEncoder).dg = function (descriptor, index, serializer, value) {
  if (this.rf(descriptor, index)) {
    this.eg(serializer, value);
  }
};
protoOf(AbstractEncoder).fg = function (descriptor, index, serializer, value) {
  if (this.rf(descriptor, index)) {
    this.gg(serializer, value);
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
  var klassName = compositeDecoder.jf($this.kc(), 0);
  var serializer = findPolymorphicSerializer_0($this, compositeDecoder, klassName);
  return compositeDecoder.lf($this.kc(), 1, serializer);
}
function AbstractPolymorphicSerializer() {
}
protoOf(AbstractPolymorphicSerializer).hd = function (encoder, value) {
  var actualSerializer = findPolymorphicSerializer(this, encoder, value);
  // Inline function 'kotlinx.serialization.encoding.encodeStructure' call
  var descriptor = this.kc();
  var composite = encoder.ef(descriptor);
  composite.cg(this.kc(), 0, actualSerializer.kc().gd());
  var tmp = this.kc();
  // Inline function 'kotlinx.serialization.internal.cast' call
  var tmp$ret$2 = isInterface(actualSerializer, SerializationStrategy) ? actualSerializer : THROW_CCE();
  composite.dg(tmp, 1, tmp$ret$2, value);
  composite.ff(descriptor);
};
protoOf(AbstractPolymorphicSerializer).mc = function (encoder, value) {
  return this.hd(encoder, !(value == null) ? value : THROW_CCE());
};
protoOf(AbstractPolymorphicSerializer).lc = function (decoder) {
  // Inline function 'kotlinx.serialization.encoding.decodeStructure' call
  var descriptor = this.kc();
  var composite = decoder.ef(descriptor);
  var tmp$ret$1;
  $l$block: {
    var klassName = null;
    var value = null;
    if (composite.of()) {
      tmp$ret$1 = decodeSequentially_0(this, composite);
      break $l$block;
    }
    mainLoop: while (true) {
      var index = composite.pf(this.kc());
      switch (index) {
        case -1:
          break mainLoop;
        case 0:
          klassName = composite.jf(this.kc(), index);
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
          value = composite.lf(this.kc(), index, serializer);
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
  composite.ff(descriptor);
  return result;
};
protoOf(AbstractPolymorphicSerializer).pc = function (decoder, klassName) {
  return decoder.nf().kg(this.oc(), klassName);
};
protoOf(AbstractPolymorphicSerializer).nc = function (encoder, value) {
  return encoder.nf().lg(this.oc(), value);
};
function throwSubtypeNotRegistered(subClass, baseClass) {
  var tmp0_elvis_lhs = subClass.j7();
  throwSubtypeNotRegistered_0(tmp0_elvis_lhs == null ? toString(subClass) : tmp0_elvis_lhs, baseClass);
}
function throwSubtypeNotRegistered_0(subClassName, baseClass) {
  var scope = "in the polymorphic scope of '" + baseClass.j7() + "'";
  throw SerializationException_init_$Create$_0(subClassName == null ? 'Class discriminator was missing and no default serializers were registered ' + scope + '.' : "Serializer for subclass '" + subClassName + "' is not found " + scope + '.\n' + ("Check if class with serial name '" + subClassName + "' exists and serializer is registered in a corresponding SerializersModule.\n") + ("To be registered automatically, class '" + subClassName + "' has to be '@Serializable', and the base class '" + baseClass.j7() + "' has to be sealed and '@Serializable'."));
}
function CachedNames() {
}
function ArrayListClassDesc(elementDesc) {
  ListLikeDescriptor.call(this, elementDesc);
}
protoOf(ArrayListClassDesc).gd = function () {
  return 'kotlin.collections.ArrayList';
};
function ListLikeDescriptor(elementDescriptor) {
  this.og_1 = elementDescriptor;
  this.pg_1 = 1;
}
protoOf(ListLikeDescriptor).rd = function () {
  return LIST_getInstance();
};
protoOf(ListLikeDescriptor).td = function () {
  return this.pg_1;
};
protoOf(ListLikeDescriptor).vd = function (index) {
  requireNonNegativeIndex(index, this.gd());
  return index.toString();
};
protoOf(ListLikeDescriptor).wd = function (name) {
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
protoOf(ListLikeDescriptor).zd = function (index) {
  requireNonNegativeIndex(index, this.gd());
  return false;
};
protoOf(ListLikeDescriptor).xd = function (index) {
  requireNonNegativeIndex(index, this.gd());
  return emptyList();
};
protoOf(ListLikeDescriptor).yd = function (index) {
  requireNonNegativeIndex(index, this.gd());
  return this.og_1;
};
protoOf(ListLikeDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof ListLikeDescriptor))
    return false;
  if (equals(this.og_1, other.og_1) && this.gd() === other.gd())
    return true;
  return false;
};
protoOf(ListLikeDescriptor).hashCode = function () {
  return imul(hashCode(this.og_1), 31) + getStringHashCode(this.gd()) | 0;
};
protoOf(ListLikeDescriptor).toString = function () {
  return this.gd() + '(' + toString(this.og_1) + ')';
};
function requireNonNegativeIndex(index, name) {
  if (index < 0)
    throw IndexOutOfBoundsException_init_$Create$('Illegal index ' + index + ', ' + name + ' expects only non-negative indices');
}
function ArrayListSerializer(element) {
  CollectionSerializer.call(this, element);
  this.rg_1 = new ArrayListClassDesc(element.kc());
}
protoOf(ArrayListSerializer).kc = function () {
  return this.rg_1;
};
protoOf(ArrayListSerializer).sg = function (initialCapacityHint) {
  if (initialCapacityHint === -1) {
    // Inline function 'kotlin.collections.arrayListOf' call
    return ArrayList_init_$Create$();
  }
  return ArrayList_init_$Create$_0(initialCapacityHint);
};
protoOf(ArrayListSerializer).tg = function (_this__u8e3s4) {
  return _this__u8e3s4.o();
};
protoOf(ArrayListSerializer).ug = function (_this__u8e3s4) {
  return this.tg(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
};
protoOf(ArrayListSerializer).vg = function (_this__u8e3s4) {
  return _this__u8e3s4;
};
protoOf(ArrayListSerializer).wg = function (_this__u8e3s4) {
  return this.vg(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
};
protoOf(ArrayListSerializer).xg = function (_this__u8e3s4, expectedSize) {
  if (_this__u8e3s4 instanceof ArrayList) {
    // Inline function 'kotlin.also' call
    _this__u8e3s4.g3(expectedSize);
    return _this__u8e3s4;
  }
  if (expectedSize === _this__u8e3s4.o())
    return ArrayList_init_$Create$_1(_this__u8e3s4);
  // Inline function 'kotlin.also' call
  var this_0 = ArrayList_init_$Create$_0(expectedSize);
  this_0.p(_this__u8e3s4);
  return this_0;
};
protoOf(ArrayListSerializer).yg = function (_this__u8e3s4, expectedSize) {
  return this.xg((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtList) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
};
protoOf(ArrayListSerializer).zg = function (_this__u8e3s4, index, element) {
  _this__u8e3s4.n2(index, element);
};
protoOf(ArrayListSerializer).ah = function (_this__u8e3s4, index, element) {
  return this.zg(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE(), index, element);
};
function CollectionSerializer(element) {
  CollectionLikeSerializer.call(this, element);
}
protoOf(CollectionSerializer).ch = function (_this__u8e3s4) {
  return _this__u8e3s4.o();
};
protoOf(CollectionSerializer).kh = function (_this__u8e3s4) {
  return this.ch((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
};
protoOf(CollectionSerializer).dh = function (_this__u8e3s4) {
  return _this__u8e3s4.j();
};
protoOf(CollectionSerializer).lh = function (_this__u8e3s4) {
  return this.dh((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
};
function CollectionLikeSerializer(elementSerializer) {
  AbstractCollectionSerializer.call(this);
  this.eh_1 = elementSerializer;
}
protoOf(CollectionLikeSerializer).fh = function (encoder, value) {
  var size = this.kh(value);
  // Inline function 'kotlinx.serialization.encoding.encodeCollection' call
  var descriptor = this.kc();
  var composite = encoder.ig(descriptor, size);
  var iterator = this.lh(value);
  var inductionVariable = 0;
  if (inductionVariable < size)
    do {
      var index = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      composite.dg(this.kc(), index, this.eh_1, iterator.l());
    }
     while (inductionVariable < size);
  composite.ff(descriptor);
};
protoOf(CollectionLikeSerializer).mc = function (encoder, value) {
  return this.fh(encoder, value);
};
protoOf(CollectionLikeSerializer).gh = function (decoder, builder, startIndex, size) {
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
      this.hh(decoder, startIndex + index | 0, builder, false);
    }
     while (inductionVariable < size);
};
protoOf(CollectionLikeSerializer).hh = function (decoder, index, builder, checkIndex) {
  this.ah(builder, index, decoder.lf(this.kc(), index, this.eh_1));
};
function AbstractCollectionSerializer() {
}
protoOf(AbstractCollectionSerializer).jh = function (decoder, previous) {
  var compositeDecoder = decoder.ef(this.kc());
  var expectedSize = compositeDecoder.qf(this.kc());
  // Inline function 'kotlin.require' call
  if (!(expectedSize >= -1)) {
    var message = 'Decoded collection size should be either non-negative, or -1 (meaning, unknown). Was: ' + expectedSize;
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  var tmp;
  if (!(previous == null)) {
    tmp = this.yg(previous, this.kh(previous) + coerceAtLeast(expectedSize, 0) | 0);
  } else {
    tmp = this.sg(expectedSize);
  }
  var builder = tmp;
  var startIndex = this.ug(builder);
  if (compositeDecoder.of()) {
    this.gh(compositeDecoder, builder, startIndex, expectedSize);
  } else {
    $l$loop: while (true) {
      var index = compositeDecoder.pf(this.kc());
      if (index === -1)
        break $l$loop;
      this.ih(compositeDecoder, startIndex + index | 0, builder);
    }
  }
  compositeDecoder.ff(this.kc());
  return this.wg(builder);
};
protoOf(AbstractCollectionSerializer).lc = function (decoder) {
  return this.jh(decoder, null);
};
protoOf(AbstractCollectionSerializer).ih = function (decoder, index, builder, checkIndex, $super) {
  checkIndex = checkIndex === VOID ? true : checkIndex;
  var tmp;
  if ($super === VOID) {
    this.hh(decoder, index, builder, checkIndex);
    tmp = Unit_instance;
  } else {
    tmp = $super.hh.call(this, decoder, index, builder, checkIndex);
  }
  return tmp;
};
function Companion() {
  Companion_instance = this;
  this.mh_1 = longArray(0);
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
  $this.qh_1[slot] = bitwiseOr($this.qh_1[slot], shiftLeft(new Long(1, 0), offsetInSlot));
}
function nextUnmarkedHighIndex($this) {
  var inductionVariable = 0;
  var last = $this.qh_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var slot = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      var slotOffset = imul(slot + 1 | 0, 64);
      var slotMarks = $this.qh_1[slot];
      while (!equalsLong(slotMarks, new Long(-1, -1))) {
        var indexInSlot = countTrailingZeroBits(invert(slotMarks));
        slotMarks = bitwiseOr(slotMarks, shiftLeft(new Long(1, 0), indexInSlot));
        var index = slotOffset + indexInSlot | 0;
        if ($this.oh_1($this.nh_1, index)) {
          $this.qh_1[slot] = slotMarks;
          return index;
        }
      }
      $this.qh_1[slot] = slotMarks;
    }
     while (inductionVariable <= last);
  return -1;
}
function ElementMarker(descriptor, readIfAbsent) {
  Companion_getInstance();
  this.nh_1 = descriptor;
  this.oh_1 = readIfAbsent;
  var elementsCount = this.nh_1.td();
  if (elementsCount <= 64) {
    var tmp = this;
    var tmp_0;
    if (elementsCount === 64) {
      tmp_0 = new Long(0, 0);
    } else {
      tmp_0 = shiftLeft(new Long(-1, -1), elementsCount);
    }
    tmp.ph_1 = tmp_0;
    this.qh_1 = Companion_getInstance().mh_1;
  } else {
    this.ph_1 = new Long(0, 0);
    this.qh_1 = prepareHighMarksArray(this, elementsCount);
  }
}
protoOf(ElementMarker).rh = function (index) {
  if (index < 64) {
    this.ph_1 = bitwiseOr(this.ph_1, shiftLeft(new Long(1, 0), index));
  } else {
    markHigh(this, index);
  }
};
protoOf(ElementMarker).sh = function () {
  var elementsCount = this.nh_1.td();
  while (!equalsLong(this.ph_1, new Long(-1, -1))) {
    var index = countTrailingZeroBits(invert(this.ph_1));
    this.ph_1 = bitwiseOr(this.ph_1, shiftLeft(new Long(1, 0), index));
    if (this.oh_1(this.nh_1, index)) {
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
  var d = new EnumDescriptor(serialName, $this.th_1.length);
  // Inline function 'kotlin.collections.forEach' call
  var indexedObject = $this.th_1;
  var inductionVariable = 0;
  var last = indexedObject.length;
  while (inductionVariable < last) {
    var element = indexedObject[inductionVariable];
    inductionVariable = inductionVariable + 1 | 0;
    d.ii(element.f1_1);
  }
  return d;
}
function EnumSerializer$descriptor$delegate$lambda(this$0, $serialName) {
  return function () {
    var tmp0_elvis_lhs = this$0.uh_1;
    return tmp0_elvis_lhs == null ? createUnmarkedDescriptor(this$0, $serialName) : tmp0_elvis_lhs;
  };
}
function EnumSerializer$_get_descriptor_$ref_j67dlw() {
  return constructCallableReference(function (p0) {
    return p0.kc();
  }, 1, 0, 2);
}
function EnumSerializer(serialName, values) {
  this.th_1 = values;
  this.uh_1 = null;
  var tmp = this;
  tmp.vh_1 = lazy_0(EnumSerializer$descriptor$delegate$lambda(this, serialName));
}
protoOf(EnumSerializer).kc = function () {
  var tmp0 = this.vh_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('descriptor', 1, tmp, EnumSerializer$_get_descriptor_$ref_j67dlw(), null);
  return tmp0.t();
};
protoOf(EnumSerializer).ji = function (encoder, value) {
  var index = indexOf(this.th_1, value);
  if (index === -1) {
    throw SerializationException_init_$Create$_0(toString(value) + ' is not a valid enum ' + this.kc().gd() + ', ' + ('must be one of ' + contentToString(this.th_1)));
  }
  encoder.yf(this.kc(), index);
};
protoOf(EnumSerializer).mc = function (encoder, value) {
  return this.ji(encoder, value instanceof Enum ? value : THROW_CCE());
};
protoOf(EnumSerializer).lc = function (decoder) {
  var index = decoder.bf(this.kc());
  if (!(0 <= index ? index <= (this.th_1.length - 1 | 0) : false)) {
    throw SerializationException_init_$Create$_0('' + index + ' is not among valid ' + this.kc().gd() + ' enum values, ' + ('values size is ' + this.th_1.length));
  }
  return this.th_1[index];
};
protoOf(EnumSerializer).toString = function () {
  return 'kotlinx.serialization.internal.EnumSerializer<' + this.kc().gd() + '>';
};
function _get_elementDescriptors__y23q9p($this) {
  var tmp0 = $this.xi_1;
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
      tmp_1[tmp_2] = buildSerialDescriptor($name + '.' + this$0.vd(tmp_2), OBJECT_getInstance(), []);
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
  this.wi_1 = ENUM_getInstance();
  var tmp = this;
  tmp.xi_1 = lazy_0(EnumDescriptor$elementDescriptors$delegate$lambda(elementsCount, name, this));
}
protoOf(EnumDescriptor).rd = function () {
  return this.wi_1;
};
protoOf(EnumDescriptor).yd = function (index) {
  return getChecked(_get_elementDescriptors__y23q9p(this), index);
};
protoOf(EnumDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (other == null)
    return false;
  if (!isInterface(other, SerialDescriptor))
    return false;
  if (!(other.rd() === ENUM_getInstance()))
    return false;
  if (!(this.gd() === other.gd()))
    return false;
  if (!equals(cachedSerialNames(this), cachedSerialNames(other)))
    return false;
  return true;
};
protoOf(EnumDescriptor).toString = function () {
  return joinToString(get_elementNames(this), ', ', this.gd() + '(', ')');
};
protoOf(EnumDescriptor).hashCode = function () {
  var result = getStringHashCode(this.gd());
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
  this.mj_1 = true;
}
protoOf(InlineClassDescriptor).sd = function () {
  return this.mj_1;
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
    if (!(this.gd() === other.gd())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(other.mj_1 && contentEquals(this.yi(), other.yi()))) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.td() === other.td())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.td();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.yd(index).gd() === other.yd(index).gd())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.yd(index).rd(), other.yd(index).rd())) {
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
  this.nj_1 = $primitiveSerializer;
}
protoOf(InlinePrimitiveDescriptor$1).oj = function () {
  // Inline function 'kotlin.arrayOf' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return [this.nj_1];
};
protoOf(InlinePrimitiveDescriptor$1).kc = function () {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
protoOf(InlinePrimitiveDescriptor$1).mc = function (encoder, value) {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
protoOf(InlinePrimitiveDescriptor$1).lc = function (decoder) {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
function missingFieldExceptionWithNewMessage(exception, message) {
  return exception.kd(message);
}
function jsonCachedSerialNames(_this__u8e3s4) {
  return cachedSerialNames(_this__u8e3s4);
}
function NullableSerializer(serializer) {
  this.qj_1 = serializer;
  this.rj_1 = new SerialDescriptorForNullable(this.qj_1.kc());
}
protoOf(NullableSerializer).kc = function () {
  return this.rj_1;
};
protoOf(NullableSerializer).sj = function (encoder, value) {
  if (!(value == null)) {
    encoder.hg();
    encoder.eg(this.qj_1, value);
  } else {
    encoder.tf();
  }
};
protoOf(NullableSerializer).mc = function (encoder, value) {
  return this.sj(encoder, value);
};
protoOf(NullableSerializer).lc = function (decoder) {
  return decoder.ve() ? decoder.df(this.qj_1) : decoder.we();
};
protoOf(NullableSerializer).equals = function (other) {
  if (this === other)
    return true;
  if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
    return false;
  if (!(other instanceof NullableSerializer))
    THROW_CCE();
  if (!equals(this.qj_1, other.qj_1))
    return false;
  return true;
};
protoOf(NullableSerializer).hashCode = function () {
  return hashCode(this.qj_1);
};
function SerialDescriptorForNullable(original) {
  this.nd_1 = original;
  this.od_1 = this.nd_1.gd() + '?';
  this.pd_1 = cachedSerialNames(this.nd_1);
}
protoOf(SerialDescriptorForNullable).gd = function () {
  return this.od_1;
};
protoOf(SerialDescriptorForNullable).te = function () {
  return this.pd_1;
};
protoOf(SerialDescriptorForNullable).ld = function () {
  return true;
};
protoOf(SerialDescriptorForNullable).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof SerialDescriptorForNullable))
    return false;
  if (!equals(this.nd_1, other.nd_1))
    return false;
  return true;
};
protoOf(SerialDescriptorForNullable).toString = function () {
  return toString(this.nd_1) + '?';
};
protoOf(SerialDescriptorForNullable).hashCode = function () {
  return imul(hashCode(this.nd_1), 31);
};
protoOf(SerialDescriptorForNullable).rd = function () {
  return this.nd_1.rd();
};
protoOf(SerialDescriptorForNullable).sd = function () {
  return this.nd_1.sd();
};
protoOf(SerialDescriptorForNullable).td = function () {
  return this.nd_1.td();
};
protoOf(SerialDescriptorForNullable).ud = function () {
  return this.nd_1.ud();
};
protoOf(SerialDescriptorForNullable).vd = function (index) {
  return this.nd_1.vd(index);
};
protoOf(SerialDescriptorForNullable).wd = function (name) {
  return this.nd_1.wd(name);
};
protoOf(SerialDescriptorForNullable).xd = function (index) {
  return this.nd_1.xd(index);
};
protoOf(SerialDescriptorForNullable).yd = function (index) {
  return this.nd_1.yd(index);
};
protoOf(SerialDescriptorForNullable).zd = function (index) {
  return this.nd_1.zd(index);
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
    return _this__u8e3s4.te();
  var result = HashSet_init_$Create$_0(_this__u8e3s4.td());
  var inductionVariable = 0;
  var last = _this__u8e3s4.td();
  if (inductionVariable < last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.plusAssign' call
      var element = _this__u8e3s4.vd(i);
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
        var element = descriptor.vd(i);
        missingFields.i(element);
      }
      missingFieldsBits = missingFieldsBits >>> 1 | 0;
    }
     while (inductionVariable < 32);
  throw MissingFieldException_init_$Create$(missingFields, descriptor.gd());
}
function hashCodeImpl(_this__u8e3s4, typeParams) {
  var result = getStringHashCode(_this__u8e3s4.gd());
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
    var tmp0_safe_receiver = element.gd();
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
    var tmp0_safe_receiver_0 = element_0.rd();
    var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode(tmp0_safe_receiver_0);
    accumulator_0 = tmp_0 + (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0) | 0;
  }
  var kindHash = accumulator_0;
  result = imul(31, result) + namesHash | 0;
  result = imul(31, result) + kindHash | 0;
  return result;
}
function toStringImpl(_this__u8e3s4) {
  var tmp = until(0, _this__u8e3s4.td());
  var tmp_0 = _this__u8e3s4.gd() + '(';
  return joinToString(tmp, ', ', tmp_0, ')', VOID, VOID, toStringImpl$lambda(_this__u8e3s4));
}
function _get_childSerializers__7vnyfa($this) {
  var tmp0 = $this.fi_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('childSerializers', 1, tmp, PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca(), null);
  return tmp0.t();
}
function _get__hashCode__tgwhef_0($this) {
  var tmp0 = $this.hi_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz(), null);
  return tmp0.t();
}
function buildIndices($this) {
  var indices = HashMap_init_$Create$();
  var inductionVariable = 0;
  var last = $this.ai_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.set' call
      var key = $this.ai_1[i];
      indices.v1(key, i);
    }
     while (inductionVariable <= last);
  return indices;
}
function PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this$0) {
  return function () {
    var tmp0_safe_receiver = this$0.xh_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.oj();
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
    var tmp0_safe_receiver = this$0.xh_1;
    var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.pj();
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
        var tmp$ret$2 = item.kc();
        destination.i(tmp$ret$2);
      }
      tmp = destination;
    }
    return compactArray(tmp);
  };
}
function PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka() {
  return constructCallableReference(function (p0) {
    return p0.yi();
  }, 1, 0, 5);
}
function PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this$0) {
  return function () {
    return hashCodeImpl(this$0, this$0.yi());
  };
}
function PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz() {
  return constructCallableReference(function (p0) {
    return _get__hashCode__tgwhef_0(p0);
  }, 1, 0, 6);
}
function PluginGeneratedSerialDescriptor(serialName, generatedSerializer, elementsCount) {
  generatedSerializer = generatedSerializer === VOID ? null : generatedSerializer;
  this.wh_1 = serialName;
  this.xh_1 = generatedSerializer;
  this.yh_1 = elementsCount;
  this.zh_1 = -1;
  var tmp = this;
  var tmp_0 = 0;
  var tmp_1 = this.yh_1;
  // Inline function 'kotlin.arrayOfNulls' call
  var tmp_2 = Array(tmp_1);
  while (tmp_0 < tmp_1) {
    tmp_2[tmp_0] = '[UNINITIALIZED]';
    tmp_0 = tmp_0 + 1 | 0;
  }
  tmp.ai_1 = tmp_2;
  var tmp_3 = this;
  // Inline function 'kotlin.arrayOfNulls' call
  var size = this.yh_1;
  tmp_3.bi_1 = Array(size);
  this.ci_1 = null;
  this.di_1 = booleanArray(this.yh_1);
  this.ei_1 = emptyMap();
  var tmp_4 = this;
  var tmp_5 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_4.fi_1 = lazy(tmp_5, PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this));
  var tmp_6 = this;
  var tmp_7 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_6.gi_1 = lazy(tmp_7, PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this));
  var tmp_8 = this;
  var tmp_9 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_8.hi_1 = lazy(tmp_9, PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this));
}
protoOf(PluginGeneratedSerialDescriptor).gd = function () {
  return this.wh_1;
};
protoOf(PluginGeneratedSerialDescriptor).td = function () {
  return this.yh_1;
};
protoOf(PluginGeneratedSerialDescriptor).rd = function () {
  return CLASS_getInstance();
};
protoOf(PluginGeneratedSerialDescriptor).ud = function () {
  var tmp0_elvis_lhs = this.ci_1;
  return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).te = function () {
  return this.ei_1.d1();
};
protoOf(PluginGeneratedSerialDescriptor).yi = function () {
  var tmp0 = this.gi_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('typeParameterDescriptors', 1, tmp, PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka(), null);
  return tmp0.t();
};
protoOf(PluginGeneratedSerialDescriptor).zi = function (name, isOptional) {
  this.zh_1 = this.zh_1 + 1 | 0;
  this.ai_1[this.zh_1] = name;
  this.di_1[this.zh_1] = isOptional;
  this.bi_1[this.zh_1] = null;
  if (this.zh_1 === (this.yh_1 - 1 | 0)) {
    this.ei_1 = buildIndices(this);
  }
};
protoOf(PluginGeneratedSerialDescriptor).ii = function (name, isOptional, $super) {
  isOptional = isOptional === VOID ? false : isOptional;
  var tmp;
  if ($super === VOID) {
    this.zi(name, isOptional);
    tmp = Unit_instance;
  } else {
    tmp = $super.zi.call(this, name, isOptional);
  }
  return tmp;
};
protoOf(PluginGeneratedSerialDescriptor).yd = function (index) {
  return getChecked(_get_childSerializers__7vnyfa(this), index).kc();
};
protoOf(PluginGeneratedSerialDescriptor).zd = function (index) {
  return getChecked_0(this.di_1, index);
};
protoOf(PluginGeneratedSerialDescriptor).xd = function (index) {
  var tmp0_elvis_lhs = getChecked(this.bi_1, index);
  return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).vd = function (index) {
  return getChecked(this.ai_1, index);
};
protoOf(PluginGeneratedSerialDescriptor).wd = function (name) {
  var tmp0_elvis_lhs = this.ei_1.c1(name);
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
    if (!(this.gd() === other.gd())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!contentEquals(this.yi(), other.yi())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.td() === other.td())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.td();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.yd(index).gd() === other.yd(index).gd())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.yd(index).rd(), other.yd(index).rd())) {
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
    return $this_toStringImpl.vd(i) + ': ' + $this_toStringImpl.yd(i).gd();
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
  this.tj_1 = new PrimitiveSerialDescriptor('kotlin.String', STRING_getInstance());
}
protoOf(StringSerializer).kc = function () {
  return this.tj_1;
};
protoOf(StringSerializer).uj = function (encoder, value) {
  return encoder.xf(value);
};
protoOf(StringSerializer).mc = function (encoder, value) {
  return this.uj(encoder, (!(value == null) ? typeof value === 'string' : false) ? value : THROW_CCE());
};
protoOf(StringSerializer).lc = function (decoder) {
  return decoder.af();
};
var StringSerializer_instance;
function StringSerializer_getInstance() {
  if (StringSerializer_instance == null)
    new StringSerializer();
  return StringSerializer_instance;
}
function error($this) {
  throw IllegalStateException_init_$Create$('Primitive descriptor ' + $this.vj_1 + ' does not have elements');
}
function PrimitiveSerialDescriptor(serialName, kind) {
  this.vj_1 = serialName;
  this.wj_1 = kind;
}
protoOf(PrimitiveSerialDescriptor).gd = function () {
  return this.vj_1;
};
protoOf(PrimitiveSerialDescriptor).rd = function () {
  return this.wj_1;
};
protoOf(PrimitiveSerialDescriptor).td = function () {
  return 0;
};
protoOf(PrimitiveSerialDescriptor).vd = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).wd = function (name) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).zd = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).yd = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).xd = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).toString = function () {
  return 'PrimitiveDescriptor(' + this.vj_1 + ')';
};
protoOf(PrimitiveSerialDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof PrimitiveSerialDescriptor))
    return false;
  if (this.vj_1 === other.vj_1 && equals(this.wj_1, other.wj_1))
    return true;
  return false;
};
protoOf(PrimitiveSerialDescriptor).hashCode = function () {
  return getStringHashCode(this.vj_1) + imul(31, this.wj_1.hashCode()) | 0;
};
function IntSerializer() {
  IntSerializer_instance = this;
  this.xj_1 = new PrimitiveSerialDescriptor('kotlin.Int', INT_getInstance());
}
protoOf(IntSerializer).kc = function () {
  return this.xj_1;
};
protoOf(IntSerializer).yj = function (encoder, value) {
  return encoder.uf(value);
};
protoOf(IntSerializer).mc = function (encoder, value) {
  return this.yj(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
};
protoOf(IntSerializer).lc = function (decoder) {
  return decoder.xe();
};
var IntSerializer_instance;
function IntSerializer_getInstance() {
  if (IntSerializer_instance == null)
    new IntSerializer();
  return IntSerializer_instance;
}
function LongSerializer() {
  LongSerializer_instance = this;
  this.zj_1 = new PrimitiveSerialDescriptor('kotlin.Long', LONG_getInstance());
}
protoOf(LongSerializer).kc = function () {
  return this.zj_1;
};
protoOf(LongSerializer).ak = function (encoder, value) {
  return encoder.vf(value);
};
protoOf(LongSerializer).mc = function (encoder, value) {
  return this.ak(encoder, value instanceof Long ? value : THROW_CCE());
};
protoOf(LongSerializer).lc = function (decoder) {
  return decoder.ye();
};
var LongSerializer_instance;
function LongSerializer_getInstance() {
  if (LongSerializer_instance == null)
    new LongSerializer();
  return LongSerializer_instance;
}
function DoubleSerializer() {
  DoubleSerializer_instance = this;
  this.bk_1 = new PrimitiveSerialDescriptor('kotlin.Double', DOUBLE_getInstance());
}
protoOf(DoubleSerializer).kc = function () {
  return this.bk_1;
};
protoOf(DoubleSerializer).ck = function (encoder, value) {
  return encoder.wf(value);
};
protoOf(DoubleSerializer).mc = function (encoder, value) {
  return this.ck(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
};
protoOf(DoubleSerializer).lc = function (decoder) {
  return decoder.ze();
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
protoOf(NamedValueDecoder).fk = function (_this__u8e3s4, index) {
  return this.hk(this.gk(_this__u8e3s4, index));
};
protoOf(NamedValueDecoder).hk = function (nestedName) {
  var tmp0_elvis_lhs = this.kk();
  return this.lk(tmp0_elvis_lhs == null ? '' : tmp0_elvis_lhs, nestedName);
};
protoOf(NamedValueDecoder).gk = function (descriptor, index) {
  return descriptor.vd(index);
};
protoOf(NamedValueDecoder).lk = function (parentName, childName) {
  var tmp;
  // Inline function 'kotlin.text.isEmpty' call
  if (charSequenceLength(parentName) === 0) {
    tmp = childName;
  } else {
    tmp = parentName + '.' + childName;
  }
  return tmp;
};
protoOf(NamedValueDecoder).mk = function () {
  return this.ik_1.m() ? '$' : joinToString(this.ik_1, '.', '$.');
};
function tagBlock($this, tag, block) {
  $this.uk(tag);
  var r = block();
  if (!$this.jk_1) {
    $this.vk();
  }
  $this.jk_1 = false;
  return r;
}
function TaggedDecoder$decodeSerializableElement$lambda(this$0, $deserializer, $previousValue) {
  return function () {
    return this$0.cf($deserializer, $previousValue);
  };
}
function TaggedDecoder$decodeNullableSerializableElement$lambda(this$0, $deserializer, $previousValue) {
  return function () {
    var tmp0 = this$0;
    // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
    var isNullabilitySupported = $deserializer.kc().ld();
    var tmp;
    if (isNullabilitySupported || tmp0.ve()) {
      tmp = this$0.cf($deserializer, $previousValue);
    } else {
      tmp = tmp0.we();
    }
    return tmp;
  };
}
function TaggedDecoder() {
  var tmp = this;
  // Inline function 'kotlin.collections.arrayListOf' call
  tmp.ik_1 = ArrayList_init_$Create$();
  this.jk_1 = false;
}
protoOf(TaggedDecoder).nf = function () {
  return EmptySerializersModule_0();
};
protoOf(TaggedDecoder).nk = function (tag) {
  throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
};
protoOf(TaggedDecoder).ok = function (tag) {
  return true;
};
protoOf(TaggedDecoder).pk = function (tag) {
  var tmp = this.nk(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).qk = function (tag) {
  var tmp = this.nk(tag);
  return tmp instanceof Long ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).rk = function (tag) {
  var tmp = this.nk(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).sk = function (tag) {
  var tmp = this.nk(tag);
  return typeof tmp === 'string' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).tk = function (tag, enumDescriptor) {
  var tmp = this.nk(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).cf = function (deserializer, previousValue) {
  return this.df(deserializer);
};
protoOf(TaggedDecoder).ve = function () {
  var tmp0_elvis_lhs = this.kk();
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return false;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var currentTag = tmp;
  return this.ok(currentTag);
};
protoOf(TaggedDecoder).we = function () {
  return null;
};
protoOf(TaggedDecoder).xe = function () {
  return this.pk(this.vk());
};
protoOf(TaggedDecoder).ye = function () {
  return this.qk(this.vk());
};
protoOf(TaggedDecoder).ze = function () {
  return this.rk(this.vk());
};
protoOf(TaggedDecoder).af = function () {
  return this.sk(this.vk());
};
protoOf(TaggedDecoder).bf = function (enumDescriptor) {
  return this.tk(this.vk(), enumDescriptor);
};
protoOf(TaggedDecoder).ef = function (descriptor) {
  return this;
};
protoOf(TaggedDecoder).ff = function (descriptor) {
};
protoOf(TaggedDecoder).gf = function (descriptor, index) {
  return this.pk(this.fk(descriptor, index));
};
protoOf(TaggedDecoder).hf = function (descriptor, index) {
  return this.qk(this.fk(descriptor, index));
};
protoOf(TaggedDecoder).if = function (descriptor, index) {
  return this.rk(this.fk(descriptor, index));
};
protoOf(TaggedDecoder).jf = function (descriptor, index) {
  return this.sk(this.fk(descriptor, index));
};
protoOf(TaggedDecoder).kf = function (descriptor, index, deserializer, previousValue) {
  var tmp = this.fk(descriptor, index);
  return tagBlock(this, tmp, TaggedDecoder$decodeSerializableElement$lambda(this, deserializer, previousValue));
};
protoOf(TaggedDecoder).mf = function (descriptor, index, deserializer, previousValue) {
  var tmp = this.fk(descriptor, index);
  return tagBlock(this, tmp, TaggedDecoder$decodeNullableSerializableElement$lambda(this, deserializer, previousValue));
};
protoOf(TaggedDecoder).kk = function () {
  return lastOrNull(this.ik_1);
};
protoOf(TaggedDecoder).uk = function (name) {
  this.ik_1.i(name);
};
protoOf(TaggedDecoder).vk = function () {
  var r = this.ik_1.f2(get_lastIndex_0(this.ik_1));
  this.jk_1 = true;
  return r;
};
function get_EmptySerializersModuleLegacyJs() {
  _init_properties_SerializersModule_kt__u78ha3();
  return EmptySerializersModule;
}
var EmptySerializersModule;
function SerializersModule() {
}
protoOf(SerializersModule).md = function (kClass, typeArgumentsSerializers, $super) {
  typeArgumentsSerializers = typeArgumentsSerializers === VOID ? emptyList() : typeArgumentsSerializers;
  return $super === VOID ? this.wk(kClass, typeArgumentsSerializers) : $super.wk.call(this, kClass, typeArgumentsSerializers);
};
function SerialModuleImpl(class2ContextualFactory, polyBase2Serializers, polyBase2DefaultSerializerProvider, polyBase2NamedSerializers, polyBase2DefaultDeserializerProvider, hasInterfaceContextualSerializers) {
  SerializersModule.call(this);
  this.xk_1 = class2ContextualFactory;
  this.yk_1 = polyBase2Serializers;
  this.zk_1 = polyBase2DefaultSerializerProvider;
  this.al_1 = polyBase2NamedSerializers;
  this.bl_1 = polyBase2DefaultDeserializerProvider;
  this.cl_1 = hasInterfaceContextualSerializers;
}
protoOf(SerialModuleImpl).lg = function (baseClass, value) {
  if (!baseClass.k7(value))
    return null;
  var tmp0_safe_receiver = this.yk_1.c1(baseClass);
  var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.c1(getKClassFromExpression(value));
  var registered = (!(tmp == null) ? isInterface(tmp, SerializationStrategy) : false) ? tmp : null;
  if (!(registered == null))
    return registered;
  var tmp_0 = this.zk_1.c1(baseClass);
  var tmp1_safe_receiver = (!(tmp_0 == null) ? typeof tmp_0 === 'function' : false) ? tmp_0 : null;
  return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(value);
};
protoOf(SerialModuleImpl).kg = function (baseClass, serializedClassName) {
  var tmp0_safe_receiver = this.al_1.c1(baseClass);
  var tmp;
  if (tmp0_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlin.collections.get' call
    tmp = (isInterface(tmp0_safe_receiver, KtMap) ? tmp0_safe_receiver : THROW_CCE()).c1(serializedClassName);
  }
  var tmp_0 = tmp;
  var registered = (!(tmp_0 == null) ? isInterface(tmp_0, KSerializer) : false) ? tmp_0 : null;
  if (!(registered == null))
    return registered;
  var tmp_1 = this.bl_1.c1(baseClass);
  var tmp1_safe_receiver = (!(tmp_1 == null) ? typeof tmp_1 === 'function' : false) ? tmp_1 : null;
  return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(serializedClassName);
};
protoOf(SerialModuleImpl).wk = function (kClass, typeArgumentsSerializers) {
  var tmp0_safe_receiver = this.xk_1.c1(kClass);
  var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.dl(typeArgumentsSerializers);
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
protoOf(SerialDescriptorImpl).ld = get_isNullable;
protoOf(SerialDescriptorImpl).sd = get_isInline;
protoOf(AbstractDecoder).lf = decodeSerializableElement$default;
protoOf(AbstractDecoder).df = decodeSerializableValue;
protoOf(AbstractDecoder).of = decodeSequentially;
protoOf(AbstractDecoder).qf = decodeCollectionSize;
protoOf(AbstractEncoder).hg = encodeNotNullMark;
protoOf(AbstractEncoder).ig = beginCollection;
protoOf(AbstractEncoder).eg = encodeSerializableValue;
protoOf(AbstractEncoder).gg = encodeNullableSerializableValue;
protoOf(AbstractEncoder).jg = shouldEncodeElementDefault;
protoOf(ListLikeDescriptor).ld = get_isNullable;
protoOf(ListLikeDescriptor).sd = get_isInline;
protoOf(ListLikeDescriptor).ud = get_annotations;
protoOf(PluginGeneratedSerialDescriptor).ld = get_isNullable;
protoOf(PluginGeneratedSerialDescriptor).sd = get_isInline;
protoOf(InlinePrimitiveDescriptor$1).pj = typeParametersSerializers;
protoOf(PrimitiveSerialDescriptor).ld = get_isNullable;
protoOf(PrimitiveSerialDescriptor).sd = get_isInline;
protoOf(PrimitiveSerialDescriptor).ud = get_annotations;
protoOf(TaggedDecoder).lf = decodeSerializableElement$default;
protoOf(TaggedDecoder).df = decodeSerializableValue;
protoOf(TaggedDecoder).of = decodeSequentially;
protoOf(TaggedDecoder).qf = decodeCollectionSize;
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

