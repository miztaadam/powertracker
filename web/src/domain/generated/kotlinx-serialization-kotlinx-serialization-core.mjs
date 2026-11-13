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
  return deserializer.nd(this);
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
  return $super === VOID ? this.mg(descriptor, index, deserializer, previousValue) : $super.mg.call(this, descriptor, index, deserializer, previousValue);
}
initMetadataForInterface(CompositeDecoder, 'CompositeDecoder');
initMetadataForClass(AbstractDecoder, 'AbstractDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
function encodeNotNullMark() {
}
function beginCollection(descriptor, collectionSize) {
  return this.gg(descriptor);
}
function encodeSerializableValue(serializer, value) {
  serializer.od(this, value);
}
function encodeNullableSerializableValue(serializer, value) {
  var isNullabilitySupported = serializer.md().ne();
  if (isNullabilitySupported) {
    return this.gh(isInterface(serializer, SerializationStrategy) ? serializer : THROW_CCE(), value);
  }
  if (value == null) {
    this.vg();
  } else {
    this.jh();
    this.gh(serializer, value);
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
  var tmp0_elvis_lhs = _this__u8e3s4.pd(encoder, value);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    throwSubtypeNotRegistered(getKClassFromExpression(value), _this__u8e3s4.qd());
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function findPolymorphicSerializer_0(_this__u8e3s4, decoder, klassName) {
  var tmp0_elvis_lhs = _this__u8e3s4.rd(decoder, klassName);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    throwSubtypeNotRegistered_0(klassName, _this__u8e3s4.qd());
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function SealedClassSerializer_init_$Init$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations, $this) {
  SealedClassSerializer.call($this, serialName, baseClass, subclasses, subclassSerializers);
  $this.td_1 = asList(classAnnotations);
  return $this;
}
function SealedClassSerializer_init_$Create$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations) {
  return SealedClassSerializer_init_$Init$(serialName, baseClass, subclasses, subclassSerializers, classAnnotations, objectCreate(protoOf(SealedClassSerializer)));
}
function SealedClassSerializer$descriptor$delegate$lambda$lambda$lambda(this$0) {
  return function ($this$buildSerialDescriptor) {
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = this$0.wd_1.t().j();
    while (_iterator__ex2g4s.k()) {
      var element = _iterator__ex2g4s.l();
      // Inline function 'kotlin.collections.component1' call
      var name = element.u();
      // Inline function 'kotlin.collections.component2' call
      var serializer = element.v();
      $this$buildSerialDescriptor.fe(name, serializer.md());
    }
    return Unit_instance;
  };
}
function SealedClassSerializer$descriptor$delegate$lambda$lambda(this$0) {
  return function ($this$buildSerialDescriptor) {
    $this$buildSerialDescriptor.fe('type', serializer(StringCompanionObject_instance).md());
    var tmp = 'kotlinx.serialization.Sealed<' + this$0.sd_1.g8() + '>';
    var tmp_0 = CONTEXTUAL_getInstance();
    var elementDescriptor = buildSerialDescriptor(tmp, tmp_0, [], SealedClassSerializer$descriptor$delegate$lambda$lambda$lambda(this$0));
    $this$buildSerialDescriptor.fe('value', elementDescriptor);
    $this$buildSerialDescriptor.zd_1 = this$0.td_1;
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
    return p0.md();
  }, 1, 0, 0);
}
function SealedClassSerializer$$inlined$groupingBy$1($this) {
  this.ge_1 = $this;
}
protoOf(SealedClassSerializer$$inlined$groupingBy$1).k2 = function () {
  return this.ge_1.j();
};
protoOf(SealedClassSerializer$$inlined$groupingBy$1).he = function (element) {
  return element.v().md().ie();
};
protoOf(SealedClassSerializer$$inlined$groupingBy$1).l2 = function (element) {
  return this.he(element);
};
function SealedClassSerializer(serialName, baseClass, subclasses, subclassSerializers) {
  AbstractPolymorphicSerializer.call(this);
  this.sd_1 = baseClass;
  this.td_1 = emptyList();
  var tmp = this;
  var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp.ud_1 = lazy(tmp_0, SealedClassSerializer$descriptor$delegate$lambda(serialName, this));
  if (!(subclasses.length === subclassSerializers.length)) {
    throw IllegalArgumentException_init_$Create$('All subclasses of sealed class ' + this.sd_1.g8() + ' should be marked @Serializable');
  }
  this.vd_1 = toMap(zip(subclasses, subclassSerializers));
  var tmp_1 = this;
  // Inline function 'kotlin.collections.groupingBy' call
  var this_0 = this.vd_1.t();
  // Inline function 'kotlin.collections.aggregate' call
  var tmp0 = new SealedClassSerializer$$inlined$groupingBy$1(this_0);
  // Inline function 'kotlin.collections.mutableMapOf' call
  // Inline function 'kotlin.collections.aggregateTo' call
  var destination = LinkedHashMap_init_$Create$();
  // Inline function 'kotlin.collections.iterator' call
  var _iterator__ex2g4s = tmp0.k2();
  while (_iterator__ex2g4s.k()) {
    var e = _iterator__ex2g4s.l();
    var key = tmp0.l2(e);
    var accumulator = destination.t1(key);
    accumulator == null && !destination.r1(key);
    if (!(accumulator == null)) {
      // Inline function 'kotlin.error' call
      var message = "Multiple sealed subclasses of '" + toString(this.sd_1) + "' have the same serial name '" + key + "':" + (" '" + toString(accumulator.u()) + "', '" + toString(e.u()) + "'");
      throw IllegalStateException_init_$Create$(toString(message));
    }
    // Inline function 'kotlin.collections.set' call
    destination.m2(key, e);
  }
  // Inline function 'kotlin.collections.mapValues' call
  // Inline function 'kotlin.collections.mapValuesTo' call
  var destination_0 = LinkedHashMap_init_$Create$_0(mapCapacity(destination.o()));
  // Inline function 'kotlin.collections.associateByTo' call
  var _iterator__ex2g4s_0 = destination.t().j();
  while (_iterator__ex2g4s_0.k()) {
    var element = _iterator__ex2g4s_0.l();
    var tmp_2 = element.u();
    var tmp$ret$12 = element.v().v();
    destination_0.m2(tmp_2, tmp$ret$12);
  }
  tmp_1.wd_1 = destination_0;
}
protoOf(SealedClassSerializer).qd = function () {
  return this.sd_1;
};
protoOf(SealedClassSerializer).md = function () {
  var tmp0 = this.ud_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('descriptor', 1, tmp, SealedClassSerializer$_get_descriptor_$ref_m511rz(), null);
  return tmp0.v();
};
protoOf(SealedClassSerializer).rd = function (decoder, klassName) {
  // Inline function 'kotlin.collections.get' call
  var this_0 = this.wd_1;
  var tmp0_elvis_lhs = (isInterface(this_0, KtMap) ? this_0 : THROW_CCE()).t1(klassName);
  return tmp0_elvis_lhs == null ? protoOf(AbstractPolymorphicSerializer).rd.call(this, decoder, klassName) : tmp0_elvis_lhs;
};
protoOf(SealedClassSerializer).pd = function (encoder, value) {
  var tmp0_elvis_lhs = this.vd_1.t1(getKClassFromExpression(value));
  var tmp1_safe_receiver = tmp0_elvis_lhs == null ? protoOf(AbstractPolymorphicSerializer).pd.call(this, encoder, value) : tmp0_elvis_lhs;
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
  this.ke_1 = missingFields;
  this.le_1 = serialName;
}
protoOf(MissingFieldException).me = function (newMessage) {
  return new MissingFieldException(newMessage, this, this.ke_1, this.le_1);
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
  if (_this__u8e3s4.md().ne()) {
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
    var tmp0_safe_receiver_0 = _this__u8e3s4.oe(tmp0_safe_receiver);
    tmp = tmp0_safe_receiver_0 == null ? null : tmp0_safe_receiver_0.md();
  }
  return tmp;
}
function get_capturedKClass(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4 instanceof ContextDescriptor) {
    tmp = _this__u8e3s4.se_1;
  } else {
    if (_this__u8e3s4 instanceof SerialDescriptorForNullable) {
      tmp = get_capturedKClass(_this__u8e3s4.pe_1);
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
  this.df_1 = $this_elementDescriptors;
  this.cf_1 = $this_elementDescriptors.ve();
}
protoOf(elementDescriptors$1).k = function () {
  return this.cf_1 > 0;
};
protoOf(elementDescriptors$1).l = function () {
  var tmp = this.df_1.ve();
  var _unary__edvuaz = this.cf_1;
  this.cf_1 = _unary__edvuaz - 1 | 0;
  return this.df_1.af(tmp - _unary__edvuaz | 0);
};
function elementDescriptors$$inlined$Iterable$1($this_elementDescriptors) {
  this.ef_1 = $this_elementDescriptors;
}
protoOf(elementDescriptors$$inlined$Iterable$1).j = function () {
  return new elementDescriptors$1(this.ef_1);
};
function elementNames$1($this_elementNames) {
  this.gf_1 = $this_elementNames;
  this.ff_1 = $this_elementNames.ve();
}
protoOf(elementNames$1).k = function () {
  return this.ff_1 > 0;
};
protoOf(elementNames$1).l = function () {
  var tmp = this.gf_1.ve();
  var _unary__edvuaz = this.ff_1;
  this.ff_1 = _unary__edvuaz - 1 | 0;
  return this.gf_1.xe(tmp - _unary__edvuaz | 0);
};
function elementNames$$inlined$Iterable$1($this_elementNames) {
  this.hf_1 = $this_elementNames;
}
protoOf(elementNames$$inlined$Iterable$1).j = function () {
  return new elementNames$1(this.hf_1);
};
function ClassSerialDescriptorBuilder(serialName) {
  this.xd_1 = serialName;
  this.yd_1 = false;
  this.zd_1 = emptyList();
  this.ae_1 = ArrayList_init_$Create$();
  this.be_1 = HashSet_init_$Create$();
  this.ce_1 = ArrayList_init_$Create$();
  this.de_1 = ArrayList_init_$Create$();
  this.ee_1 = ArrayList_init_$Create$();
}
protoOf(ClassSerialDescriptorBuilder).if = function (elementName, descriptor, annotations, isOptional) {
  // Inline function 'kotlin.require' call
  if (!this.be_1.i(elementName)) {
    var message = "Element with name '" + elementName + "' is already registered in " + this.xd_1;
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  // Inline function 'kotlin.collections.plusAssign' call
  this.ae_1.i(elementName);
  // Inline function 'kotlin.collections.plusAssign' call
  this.ce_1.i(descriptor);
  // Inline function 'kotlin.collections.plusAssign' call
  this.de_1.i(annotations);
  // Inline function 'kotlin.collections.plusAssign' call
  this.ee_1.i(isOptional);
};
protoOf(ClassSerialDescriptorBuilder).fe = function (elementName, descriptor, annotations, isOptional, $super) {
  annotations = annotations === VOID ? emptyList() : annotations;
  isOptional = isOptional === VOID ? false : isOptional;
  var tmp;
  if ($super === VOID) {
    this.if(elementName, descriptor, annotations, isOptional);
    tmp = Unit_instance;
  } else {
    tmp = $super.if.call(this, elementName, descriptor, annotations, isOptional);
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
  return new SerialDescriptorImpl(serialName, kind, sdBuilder.ae_1.o(), toList(typeParameters), sdBuilder);
}
function _get__hashCode__tgwhef($this) {
  var tmp0 = $this.uf_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp(), null);
  return tmp0.v();
}
function SerialDescriptorImpl$_hashCode$delegate$lambda(this$0) {
  return function () {
    return hashCodeImpl(this$0, this$0.tf_1);
  };
}
function SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp() {
  return constructCallableReference(function (p0) {
    return _get__hashCode__tgwhef(p0);
  }, 1, 0, 1);
}
function SerialDescriptorImpl(serialName, kind, elementsCount, typeParameters, builder) {
  this.jf_1 = serialName;
  this.kf_1 = kind;
  this.lf_1 = elementsCount;
  this.mf_1 = builder.zd_1;
  this.nf_1 = toHashSet(builder.ae_1);
  var tmp = this;
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = builder.ae_1;
  tmp.of_1 = copyToArray(this_0);
  this.pf_1 = compactArray(builder.ce_1);
  var tmp_0 = this;
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_1 = builder.de_1;
  tmp_0.qf_1 = copyToArray(this_1);
  this.rf_1 = toBooleanArray(builder.ee_1);
  var tmp_1 = this;
  // Inline function 'kotlin.collections.map' call
  var this_2 = withIndex(this.of_1);
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$_0(collectionSizeOrDefault(this_2, 10));
  var _iterator__ex2g4s = this_2.j();
  while (_iterator__ex2g4s.k()) {
    var item = _iterator__ex2g4s.l();
    var tmp$ret$4 = to(item.qa_1, item.pa_1);
    destination.i(tmp$ret$4);
  }
  tmp_1.sf_1 = toMap(destination);
  this.tf_1 = compactArray(typeParameters);
  var tmp_2 = this;
  tmp_2.uf_1 = lazy_0(SerialDescriptorImpl$_hashCode$delegate$lambda(this));
}
protoOf(SerialDescriptorImpl).ie = function () {
  return this.jf_1;
};
protoOf(SerialDescriptorImpl).te = function () {
  return this.kf_1;
};
protoOf(SerialDescriptorImpl).ve = function () {
  return this.lf_1;
};
protoOf(SerialDescriptorImpl).we = function () {
  return this.mf_1;
};
protoOf(SerialDescriptorImpl).vf = function () {
  return this.nf_1;
};
protoOf(SerialDescriptorImpl).xe = function (index) {
  return getChecked(this.of_1, index);
};
protoOf(SerialDescriptorImpl).ye = function (name) {
  var tmp0_elvis_lhs = this.sf_1.t1(name);
  return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
};
protoOf(SerialDescriptorImpl).ze = function (index) {
  return getChecked(this.qf_1, index);
};
protoOf(SerialDescriptorImpl).af = function (index) {
  return getChecked(this.pf_1, index);
};
protoOf(SerialDescriptorImpl).bf = function (index) {
  return getChecked_0(this.rf_1, index);
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
    if (!(this.ie() === other.ie())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!contentEquals(this.tf_1, other.tf_1)) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.ve() === other.ve())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.ve();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.af(index).ie() === other.af(index).ie())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.af(index).te(), other.af(index).te())) {
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
  return ensureNotNull(getKClassFromExpression(this).g8());
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
protoOf(AbstractDecoder).wf = function () {
  throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
};
protoOf(AbstractDecoder).xf = function () {
  return true;
};
protoOf(AbstractDecoder).yf = function () {
  return null;
};
protoOf(AbstractDecoder).zf = function () {
  var tmp = this.wf();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).ag = function () {
  var tmp = this.wf();
  return tmp instanceof Long ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).bg = function () {
  var tmp = this.wf();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).cg = function () {
  var tmp = this.wf();
  return typeof tmp === 'string' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).dg = function (enumDescriptor) {
  var tmp = this.wf();
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(AbstractDecoder).eg = function (deserializer, previousValue) {
  return this.fg(deserializer);
};
protoOf(AbstractDecoder).gg = function (descriptor) {
  return this;
};
protoOf(AbstractDecoder).hg = function (descriptor) {
};
protoOf(AbstractDecoder).ig = function (descriptor, index) {
  return this.zf();
};
protoOf(AbstractDecoder).jg = function (descriptor, index) {
  return this.ag();
};
protoOf(AbstractDecoder).kg = function (descriptor, index) {
  return this.bg();
};
protoOf(AbstractDecoder).lg = function (descriptor, index) {
  return this.cg();
};
protoOf(AbstractDecoder).mg = function (descriptor, index, deserializer, previousValue) {
  return this.eg(deserializer, previousValue);
};
protoOf(AbstractDecoder).og = function (descriptor, index, deserializer, previousValue) {
  // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
  var isNullabilitySupported = deserializer.md().ne();
  var tmp;
  if (isNullabilitySupported || this.xf()) {
    tmp = this.eg(deserializer, previousValue);
  } else {
    tmp = this.yf();
  }
  return tmp;
};
function AbstractEncoder() {
}
protoOf(AbstractEncoder).gg = function (descriptor) {
  return this;
};
protoOf(AbstractEncoder).hg = function (descriptor) {
};
protoOf(AbstractEncoder).tg = function (descriptor, index) {
  return true;
};
protoOf(AbstractEncoder).ug = function (value) {
  throw SerializationException_init_$Create$_0('Non-serializable ' + toString(getKClassFromExpression(value)) + ' is not supported by ' + toString(getKClassFromExpression(this)) + ' encoder');
};
protoOf(AbstractEncoder).vg = function () {
  throw SerializationException_init_$Create$_0("'null' is not supported by default");
};
protoOf(AbstractEncoder).wg = function (value) {
  return this.ug(value);
};
protoOf(AbstractEncoder).xg = function (value) {
  return this.ug(value);
};
protoOf(AbstractEncoder).yg = function (value) {
  return this.ug(value);
};
protoOf(AbstractEncoder).zg = function (value) {
  return this.ug(value);
};
protoOf(AbstractEncoder).ah = function (enumDescriptor, index) {
  return this.ug(index);
};
protoOf(AbstractEncoder).bh = function (descriptor, index, value) {
  if (this.tg(descriptor, index)) {
    this.wg(value);
  }
};
protoOf(AbstractEncoder).ch = function (descriptor, index, value) {
  if (this.tg(descriptor, index)) {
    this.xg(value);
  }
};
protoOf(AbstractEncoder).dh = function (descriptor, index, value) {
  if (this.tg(descriptor, index)) {
    this.yg(value);
  }
};
protoOf(AbstractEncoder).eh = function (descriptor, index, value) {
  if (this.tg(descriptor, index)) {
    this.zg(value);
  }
};
protoOf(AbstractEncoder).fh = function (descriptor, index, serializer, value) {
  if (this.tg(descriptor, index)) {
    this.gh(serializer, value);
  }
};
protoOf(AbstractEncoder).hh = function (descriptor, index, serializer, value) {
  if (this.tg(descriptor, index)) {
    this.ih(serializer, value);
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
  var klassName = compositeDecoder.lg($this.md(), 0);
  var serializer = findPolymorphicSerializer_0($this, compositeDecoder, klassName);
  return compositeDecoder.ng($this.md(), 1, serializer);
}
function AbstractPolymorphicSerializer() {
}
protoOf(AbstractPolymorphicSerializer).je = function (encoder, value) {
  var actualSerializer = findPolymorphicSerializer(this, encoder, value);
  // Inline function 'kotlinx.serialization.encoding.encodeStructure' call
  var descriptor = this.md();
  var composite = encoder.gg(descriptor);
  composite.eh(this.md(), 0, actualSerializer.md().ie());
  var tmp = this.md();
  // Inline function 'kotlinx.serialization.internal.cast' call
  var tmp$ret$2 = isInterface(actualSerializer, SerializationStrategy) ? actualSerializer : THROW_CCE();
  composite.fh(tmp, 1, tmp$ret$2, value);
  composite.hg(descriptor);
};
protoOf(AbstractPolymorphicSerializer).od = function (encoder, value) {
  return this.je(encoder, !(value == null) ? value : THROW_CCE());
};
protoOf(AbstractPolymorphicSerializer).nd = function (decoder) {
  // Inline function 'kotlinx.serialization.encoding.decodeStructure' call
  var descriptor = this.md();
  var composite = decoder.gg(descriptor);
  var tmp$ret$1;
  $l$block: {
    var klassName = null;
    var value = null;
    if (composite.qg()) {
      tmp$ret$1 = decodeSequentially_0(this, composite);
      break $l$block;
    }
    mainLoop: while (true) {
      var index = composite.rg(this.md());
      switch (index) {
        case -1:
          break mainLoop;
        case 0:
          klassName = composite.lg(this.md(), index);
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
          value = composite.ng(this.md(), index, serializer);
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
  composite.hg(descriptor);
  return result;
};
protoOf(AbstractPolymorphicSerializer).rd = function (decoder, klassName) {
  return decoder.pg().mh(this.qd(), klassName);
};
protoOf(AbstractPolymorphicSerializer).pd = function (encoder, value) {
  return encoder.pg().nh(this.qd(), value);
};
function throwSubtypeNotRegistered(subClass, baseClass) {
  var tmp0_elvis_lhs = subClass.g8();
  throwSubtypeNotRegistered_0(tmp0_elvis_lhs == null ? toString(subClass) : tmp0_elvis_lhs, baseClass);
}
function throwSubtypeNotRegistered_0(subClassName, baseClass) {
  var scope = "in the polymorphic scope of '" + baseClass.g8() + "'";
  throw SerializationException_init_$Create$_0(subClassName == null ? 'Class discriminator was missing and no default serializers were registered ' + scope + '.' : "Serializer for subclass '" + subClassName + "' is not found " + scope + '.\n' + ("Check if class with serial name '" + subClassName + "' exists and serializer is registered in a corresponding SerializersModule.\n") + ("To be registered automatically, class '" + subClassName + "' has to be '@Serializable', and the base class '" + baseClass.g8() + "' has to be sealed and '@Serializable'."));
}
function CachedNames() {
}
function ArrayListClassDesc(elementDesc) {
  ListLikeDescriptor.call(this, elementDesc);
}
protoOf(ArrayListClassDesc).ie = function () {
  return 'kotlin.collections.ArrayList';
};
function ListLikeDescriptor(elementDescriptor) {
  this.qh_1 = elementDescriptor;
  this.rh_1 = 1;
}
protoOf(ListLikeDescriptor).te = function () {
  return LIST_getInstance();
};
protoOf(ListLikeDescriptor).ve = function () {
  return this.rh_1;
};
protoOf(ListLikeDescriptor).xe = function (index) {
  requireNonNegativeIndex(index, this.ie());
  return index.toString();
};
protoOf(ListLikeDescriptor).ye = function (name) {
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
protoOf(ListLikeDescriptor).bf = function (index) {
  requireNonNegativeIndex(index, this.ie());
  return false;
};
protoOf(ListLikeDescriptor).ze = function (index) {
  requireNonNegativeIndex(index, this.ie());
  return emptyList();
};
protoOf(ListLikeDescriptor).af = function (index) {
  requireNonNegativeIndex(index, this.ie());
  return this.qh_1;
};
protoOf(ListLikeDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof ListLikeDescriptor))
    return false;
  if (equals(this.qh_1, other.qh_1) && this.ie() === other.ie())
    return true;
  return false;
};
protoOf(ListLikeDescriptor).hashCode = function () {
  return imul(hashCode(this.qh_1), 31) + getStringHashCode(this.ie()) | 0;
};
protoOf(ListLikeDescriptor).toString = function () {
  return this.ie() + '(' + toString(this.qh_1) + ')';
};
function requireNonNegativeIndex(index, name) {
  if (index < 0)
    throw IndexOutOfBoundsException_init_$Create$('Illegal index ' + index + ', ' + name + ' expects only non-negative indices');
}
function ArrayListSerializer(element) {
  CollectionSerializer.call(this, element);
  this.th_1 = new ArrayListClassDesc(element.md());
}
protoOf(ArrayListSerializer).md = function () {
  return this.th_1;
};
protoOf(ArrayListSerializer).uh = function (initialCapacityHint) {
  if (initialCapacityHint === -1) {
    // Inline function 'kotlin.collections.arrayListOf' call
    return ArrayList_init_$Create$();
  }
  return ArrayList_init_$Create$_0(initialCapacityHint);
};
protoOf(ArrayListSerializer).vh = function (_this__u8e3s4) {
  return _this__u8e3s4.o();
};
protoOf(ArrayListSerializer).wh = function (_this__u8e3s4) {
  return this.vh(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
};
protoOf(ArrayListSerializer).xh = function (_this__u8e3s4) {
  return _this__u8e3s4;
};
protoOf(ArrayListSerializer).yh = function (_this__u8e3s4) {
  return this.xh(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
};
protoOf(ArrayListSerializer).zh = function (_this__u8e3s4, expectedSize) {
  if (_this__u8e3s4 instanceof ArrayList) {
    // Inline function 'kotlin.also' call
    _this__u8e3s4.b4(expectedSize);
    return _this__u8e3s4;
  }
  if (expectedSize === _this__u8e3s4.o())
    return ArrayList_init_$Create$_1(_this__u8e3s4);
  // Inline function 'kotlin.also' call
  var this_0 = ArrayList_init_$Create$_0(expectedSize);
  this_0.p(_this__u8e3s4);
  return this_0;
};
protoOf(ArrayListSerializer).ai = function (_this__u8e3s4, expectedSize) {
  return this.zh((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtList) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
};
protoOf(ArrayListSerializer).bi = function (_this__u8e3s4, index, element) {
  _this__u8e3s4.i3(index, element);
};
protoOf(ArrayListSerializer).ci = function (_this__u8e3s4, index, element) {
  return this.bi(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE(), index, element);
};
function CollectionSerializer(element) {
  CollectionLikeSerializer.call(this, element);
}
protoOf(CollectionSerializer).ei = function (_this__u8e3s4) {
  return _this__u8e3s4.o();
};
protoOf(CollectionSerializer).mi = function (_this__u8e3s4) {
  return this.ei((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
};
protoOf(CollectionSerializer).fi = function (_this__u8e3s4) {
  return _this__u8e3s4.j();
};
protoOf(CollectionSerializer).ni = function (_this__u8e3s4) {
  return this.fi((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
};
function CollectionLikeSerializer(elementSerializer) {
  AbstractCollectionSerializer.call(this);
  this.gi_1 = elementSerializer;
}
protoOf(CollectionLikeSerializer).hi = function (encoder, value) {
  var size = this.mi(value);
  // Inline function 'kotlinx.serialization.encoding.encodeCollection' call
  var descriptor = this.md();
  var composite = encoder.kh(descriptor, size);
  var iterator = this.ni(value);
  var inductionVariable = 0;
  if (inductionVariable < size)
    do {
      var index = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      composite.fh(this.md(), index, this.gi_1, iterator.l());
    }
     while (inductionVariable < size);
  composite.hg(descriptor);
};
protoOf(CollectionLikeSerializer).od = function (encoder, value) {
  return this.hi(encoder, value);
};
protoOf(CollectionLikeSerializer).ii = function (decoder, builder, startIndex, size) {
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
      this.ji(decoder, startIndex + index | 0, builder, false);
    }
     while (inductionVariable < size);
};
protoOf(CollectionLikeSerializer).ji = function (decoder, index, builder, checkIndex) {
  this.ci(builder, index, decoder.ng(this.md(), index, this.gi_1));
};
function AbstractCollectionSerializer() {
}
protoOf(AbstractCollectionSerializer).li = function (decoder, previous) {
  var compositeDecoder = decoder.gg(this.md());
  var expectedSize = compositeDecoder.sg(this.md());
  // Inline function 'kotlin.require' call
  if (!(expectedSize >= -1)) {
    var message = 'Decoded collection size should be either non-negative, or -1 (meaning, unknown). Was: ' + expectedSize;
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  var tmp;
  if (!(previous == null)) {
    tmp = this.ai(previous, this.mi(previous) + coerceAtLeast(expectedSize, 0) | 0);
  } else {
    tmp = this.uh(expectedSize);
  }
  var builder = tmp;
  var startIndex = this.wh(builder);
  if (compositeDecoder.qg()) {
    this.ii(compositeDecoder, builder, startIndex, expectedSize);
  } else {
    $l$loop: while (true) {
      var index = compositeDecoder.rg(this.md());
      if (index === -1)
        break $l$loop;
      this.ki(compositeDecoder, startIndex + index | 0, builder);
    }
  }
  compositeDecoder.hg(this.md());
  return this.yh(builder);
};
protoOf(AbstractCollectionSerializer).nd = function (decoder) {
  return this.li(decoder, null);
};
protoOf(AbstractCollectionSerializer).ki = function (decoder, index, builder, checkIndex, $super) {
  checkIndex = checkIndex === VOID ? true : checkIndex;
  var tmp;
  if ($super === VOID) {
    this.ji(decoder, index, builder, checkIndex);
    tmp = Unit_instance;
  } else {
    tmp = $super.ji.call(this, decoder, index, builder, checkIndex);
  }
  return tmp;
};
function Companion() {
  Companion_instance = this;
  this.oi_1 = longArray(0);
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
  $this.si_1[slot] = bitwiseOr($this.si_1[slot], shiftLeft(new Long(1, 0), offsetInSlot));
}
function nextUnmarkedHighIndex($this) {
  var inductionVariable = 0;
  var last = $this.si_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var slot = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      var slotOffset = imul(slot + 1 | 0, 64);
      var slotMarks = $this.si_1[slot];
      while (!equalsLong(slotMarks, new Long(-1, -1))) {
        var indexInSlot = countTrailingZeroBits(invert(slotMarks));
        slotMarks = bitwiseOr(slotMarks, shiftLeft(new Long(1, 0), indexInSlot));
        var index = slotOffset + indexInSlot | 0;
        if ($this.qi_1($this.pi_1, index)) {
          $this.si_1[slot] = slotMarks;
          return index;
        }
      }
      $this.si_1[slot] = slotMarks;
    }
     while (inductionVariable <= last);
  return -1;
}
function ElementMarker(descriptor, readIfAbsent) {
  Companion_getInstance();
  this.pi_1 = descriptor;
  this.qi_1 = readIfAbsent;
  var elementsCount = this.pi_1.ve();
  if (elementsCount <= 64) {
    var tmp = this;
    var tmp_0;
    if (elementsCount === 64) {
      tmp_0 = new Long(0, 0);
    } else {
      tmp_0 = shiftLeft(new Long(-1, -1), elementsCount);
    }
    tmp.ri_1 = tmp_0;
    this.si_1 = Companion_getInstance().oi_1;
  } else {
    this.ri_1 = new Long(0, 0);
    this.si_1 = prepareHighMarksArray(this, elementsCount);
  }
}
protoOf(ElementMarker).ti = function (index) {
  if (index < 64) {
    this.ri_1 = bitwiseOr(this.ri_1, shiftLeft(new Long(1, 0), index));
  } else {
    markHigh(this, index);
  }
};
protoOf(ElementMarker).ui = function () {
  var elementsCount = this.pi_1.ve();
  while (!equalsLong(this.ri_1, new Long(-1, -1))) {
    var index = countTrailingZeroBits(invert(this.ri_1));
    this.ri_1 = bitwiseOr(this.ri_1, shiftLeft(new Long(1, 0), index));
    if (this.qi_1(this.pi_1, index)) {
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
  var d = new EnumDescriptor(serialName, $this.vi_1.length);
  // Inline function 'kotlin.collections.forEach' call
  var indexedObject = $this.vi_1;
  var inductionVariable = 0;
  var last = indexedObject.length;
  while (inductionVariable < last) {
    var element = indexedObject[inductionVariable];
    inductionVariable = inductionVariable + 1 | 0;
    d.kj(element.w1_1);
  }
  return d;
}
function EnumSerializer$descriptor$delegate$lambda(this$0, $serialName) {
  return function () {
    var tmp0_elvis_lhs = this$0.wi_1;
    return tmp0_elvis_lhs == null ? createUnmarkedDescriptor(this$0, $serialName) : tmp0_elvis_lhs;
  };
}
function EnumSerializer$_get_descriptor_$ref_j67dlw() {
  return constructCallableReference(function (p0) {
    return p0.md();
  }, 1, 0, 2);
}
function EnumSerializer(serialName, values) {
  this.vi_1 = values;
  this.wi_1 = null;
  var tmp = this;
  tmp.xi_1 = lazy_0(EnumSerializer$descriptor$delegate$lambda(this, serialName));
}
protoOf(EnumSerializer).md = function () {
  var tmp0 = this.xi_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('descriptor', 1, tmp, EnumSerializer$_get_descriptor_$ref_j67dlw(), null);
  return tmp0.v();
};
protoOf(EnumSerializer).lj = function (encoder, value) {
  var index = indexOf(this.vi_1, value);
  if (index === -1) {
    throw SerializationException_init_$Create$_0(toString(value) + ' is not a valid enum ' + this.md().ie() + ', ' + ('must be one of ' + contentToString(this.vi_1)));
  }
  encoder.ah(this.md(), index);
};
protoOf(EnumSerializer).od = function (encoder, value) {
  return this.lj(encoder, value instanceof Enum ? value : THROW_CCE());
};
protoOf(EnumSerializer).nd = function (decoder) {
  var index = decoder.dg(this.md());
  if (!(0 <= index ? index <= (this.vi_1.length - 1 | 0) : false)) {
    throw SerializationException_init_$Create$_0('' + index + ' is not among valid ' + this.md().ie() + ' enum values, ' + ('values size is ' + this.vi_1.length));
  }
  return this.vi_1[index];
};
protoOf(EnumSerializer).toString = function () {
  return 'kotlinx.serialization.internal.EnumSerializer<' + this.md().ie() + '>';
};
function _get_elementDescriptors__y23q9p($this) {
  var tmp0 = $this.zj_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('elementDescriptors', 1, tmp, EnumDescriptor$_get_elementDescriptors_$ref_5lvk4a(), null);
  return tmp0.v();
}
function EnumDescriptor$elementDescriptors$delegate$lambda($elementsCount, $name, this$0) {
  return function () {
    var tmp = 0;
    var tmp_0 = $elementsCount;
    // Inline function 'kotlin.arrayOfNulls' call
    var tmp_1 = Array(tmp_0);
    while (tmp < tmp_0) {
      var tmp_2 = tmp;
      tmp_1[tmp_2] = buildSerialDescriptor($name + '.' + this$0.xe(tmp_2), OBJECT_getInstance(), []);
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
  this.yj_1 = ENUM_getInstance();
  var tmp = this;
  tmp.zj_1 = lazy_0(EnumDescriptor$elementDescriptors$delegate$lambda(elementsCount, name, this));
}
protoOf(EnumDescriptor).te = function () {
  return this.yj_1;
};
protoOf(EnumDescriptor).af = function (index) {
  return getChecked(_get_elementDescriptors__y23q9p(this), index);
};
protoOf(EnumDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (other == null)
    return false;
  if (!isInterface(other, SerialDescriptor))
    return false;
  if (!(other.te() === ENUM_getInstance()))
    return false;
  if (!(this.ie() === other.ie()))
    return false;
  if (!equals(cachedSerialNames(this), cachedSerialNames(other)))
    return false;
  return true;
};
protoOf(EnumDescriptor).toString = function () {
  return joinToString(get_elementNames(this), ', ', this.ie() + '(', ')');
};
protoOf(EnumDescriptor).hashCode = function () {
  var result = getStringHashCode(this.ie());
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
  this.ok_1 = true;
}
protoOf(InlineClassDescriptor).ue = function () {
  return this.ok_1;
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
    if (!(this.ie() === other.ie())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(other.ok_1 && contentEquals(this.ak(), other.ak()))) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.ve() === other.ve())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.ve();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.af(index).ie() === other.af(index).ie())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.af(index).te(), other.af(index).te())) {
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
  this.pk_1 = $primitiveSerializer;
}
protoOf(InlinePrimitiveDescriptor$1).qk = function () {
  // Inline function 'kotlin.arrayOf' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return [this.pk_1];
};
protoOf(InlinePrimitiveDescriptor$1).md = function () {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
protoOf(InlinePrimitiveDescriptor$1).od = function (encoder, value) {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
protoOf(InlinePrimitiveDescriptor$1).nd = function (decoder) {
  // Inline function 'kotlin.error' call
  var message = 'unsupported';
  throw IllegalStateException_init_$Create$(toString(message));
};
function missingFieldExceptionWithNewMessage(exception, message) {
  return exception.me(message);
}
function jsonCachedSerialNames(_this__u8e3s4) {
  return cachedSerialNames(_this__u8e3s4);
}
function NullableSerializer(serializer) {
  this.sk_1 = serializer;
  this.tk_1 = new SerialDescriptorForNullable(this.sk_1.md());
}
protoOf(NullableSerializer).md = function () {
  return this.tk_1;
};
protoOf(NullableSerializer).uk = function (encoder, value) {
  if (!(value == null)) {
    encoder.jh();
    encoder.gh(this.sk_1, value);
  } else {
    encoder.vg();
  }
};
protoOf(NullableSerializer).od = function (encoder, value) {
  return this.uk(encoder, value);
};
protoOf(NullableSerializer).nd = function (decoder) {
  return decoder.xf() ? decoder.fg(this.sk_1) : decoder.yf();
};
protoOf(NullableSerializer).equals = function (other) {
  if (this === other)
    return true;
  if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
    return false;
  if (!(other instanceof NullableSerializer))
    THROW_CCE();
  if (!equals(this.sk_1, other.sk_1))
    return false;
  return true;
};
protoOf(NullableSerializer).hashCode = function () {
  return hashCode(this.sk_1);
};
function SerialDescriptorForNullable(original) {
  this.pe_1 = original;
  this.qe_1 = this.pe_1.ie() + '?';
  this.re_1 = cachedSerialNames(this.pe_1);
}
protoOf(SerialDescriptorForNullable).ie = function () {
  return this.qe_1;
};
protoOf(SerialDescriptorForNullable).vf = function () {
  return this.re_1;
};
protoOf(SerialDescriptorForNullable).ne = function () {
  return true;
};
protoOf(SerialDescriptorForNullable).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof SerialDescriptorForNullable))
    return false;
  if (!equals(this.pe_1, other.pe_1))
    return false;
  return true;
};
protoOf(SerialDescriptorForNullable).toString = function () {
  return toString(this.pe_1) + '?';
};
protoOf(SerialDescriptorForNullable).hashCode = function () {
  return imul(hashCode(this.pe_1), 31);
};
protoOf(SerialDescriptorForNullable).te = function () {
  return this.pe_1.te();
};
protoOf(SerialDescriptorForNullable).ue = function () {
  return this.pe_1.ue();
};
protoOf(SerialDescriptorForNullable).ve = function () {
  return this.pe_1.ve();
};
protoOf(SerialDescriptorForNullable).we = function () {
  return this.pe_1.we();
};
protoOf(SerialDescriptorForNullable).xe = function (index) {
  return this.pe_1.xe(index);
};
protoOf(SerialDescriptorForNullable).ye = function (name) {
  return this.pe_1.ye(name);
};
protoOf(SerialDescriptorForNullable).ze = function (index) {
  return this.pe_1.ze(index);
};
protoOf(SerialDescriptorForNullable).af = function (index) {
  return this.pe_1.af(index);
};
protoOf(SerialDescriptorForNullable).bf = function (index) {
  return this.pe_1.bf(index);
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
    return _this__u8e3s4.vf();
  var result = HashSet_init_$Create$_0(_this__u8e3s4.ve());
  var inductionVariable = 0;
  var last = _this__u8e3s4.ve();
  if (inductionVariable < last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.plusAssign' call
      var element = _this__u8e3s4.xe(i);
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
        var element = descriptor.xe(i);
        missingFields.i(element);
      }
      missingFieldsBits = missingFieldsBits >>> 1 | 0;
    }
     while (inductionVariable < 32);
  throw MissingFieldException_init_$Create$(missingFields, descriptor.ie());
}
function hashCodeImpl(_this__u8e3s4, typeParams) {
  var result = getStringHashCode(_this__u8e3s4.ie());
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
    var tmp0_safe_receiver = element.ie();
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
    var tmp0_safe_receiver_0 = element_0.te();
    var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode(tmp0_safe_receiver_0);
    accumulator_0 = tmp_0 + (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0) | 0;
  }
  var kindHash = accumulator_0;
  result = imul(31, result) + namesHash | 0;
  result = imul(31, result) + kindHash | 0;
  return result;
}
function toStringImpl(_this__u8e3s4) {
  var tmp = until(0, _this__u8e3s4.ve());
  var tmp_0 = _this__u8e3s4.ie() + '(';
  return joinToString(tmp, ', ', tmp_0, ')', VOID, VOID, toStringImpl$lambda(_this__u8e3s4));
}
function _get_childSerializers__7vnyfa($this) {
  var tmp0 = $this.hj_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('childSerializers', 1, tmp, PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca(), null);
  return tmp0.v();
}
function _get__hashCode__tgwhef_0($this) {
  var tmp0 = $this.jj_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz(), null);
  return tmp0.v();
}
function buildIndices($this) {
  var indices = HashMap_init_$Create$();
  var inductionVariable = 0;
  var last = $this.cj_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.set' call
      var key = $this.cj_1[i];
      indices.m2(key, i);
    }
     while (inductionVariable <= last);
  return indices;
}
function PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this$0) {
  return function () {
    var tmp0_safe_receiver = this$0.zi_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.qk();
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
    var tmp0_safe_receiver = this$0.zi_1;
    var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.rk();
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
        var tmp$ret$2 = item.md();
        destination.i(tmp$ret$2);
      }
      tmp = destination;
    }
    return compactArray(tmp);
  };
}
function PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka() {
  return constructCallableReference(function (p0) {
    return p0.ak();
  }, 1, 0, 5);
}
function PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this$0) {
  return function () {
    return hashCodeImpl(this$0, this$0.ak());
  };
}
function PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz() {
  return constructCallableReference(function (p0) {
    return _get__hashCode__tgwhef_0(p0);
  }, 1, 0, 6);
}
function PluginGeneratedSerialDescriptor(serialName, generatedSerializer, elementsCount) {
  generatedSerializer = generatedSerializer === VOID ? null : generatedSerializer;
  this.yi_1 = serialName;
  this.zi_1 = generatedSerializer;
  this.aj_1 = elementsCount;
  this.bj_1 = -1;
  var tmp = this;
  var tmp_0 = 0;
  var tmp_1 = this.aj_1;
  // Inline function 'kotlin.arrayOfNulls' call
  var tmp_2 = Array(tmp_1);
  while (tmp_0 < tmp_1) {
    tmp_2[tmp_0] = '[UNINITIALIZED]';
    tmp_0 = tmp_0 + 1 | 0;
  }
  tmp.cj_1 = tmp_2;
  var tmp_3 = this;
  // Inline function 'kotlin.arrayOfNulls' call
  var size = this.aj_1;
  tmp_3.dj_1 = Array(size);
  this.ej_1 = null;
  this.fj_1 = booleanArray(this.aj_1);
  this.gj_1 = emptyMap();
  var tmp_4 = this;
  var tmp_5 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_4.hj_1 = lazy(tmp_5, PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this));
  var tmp_6 = this;
  var tmp_7 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_6.ij_1 = lazy(tmp_7, PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this));
  var tmp_8 = this;
  var tmp_9 = LazyThreadSafetyMode_PUBLICATION_getInstance();
  tmp_8.jj_1 = lazy(tmp_9, PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this));
}
protoOf(PluginGeneratedSerialDescriptor).ie = function () {
  return this.yi_1;
};
protoOf(PluginGeneratedSerialDescriptor).ve = function () {
  return this.aj_1;
};
protoOf(PluginGeneratedSerialDescriptor).te = function () {
  return CLASS_getInstance();
};
protoOf(PluginGeneratedSerialDescriptor).we = function () {
  var tmp0_elvis_lhs = this.ej_1;
  return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).vf = function () {
  return this.gj_1.u1();
};
protoOf(PluginGeneratedSerialDescriptor).ak = function () {
  var tmp0 = this.ij_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('typeParameterDescriptors', 1, tmp, PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka(), null);
  return tmp0.v();
};
protoOf(PluginGeneratedSerialDescriptor).bk = function (name, isOptional) {
  this.bj_1 = this.bj_1 + 1 | 0;
  this.cj_1[this.bj_1] = name;
  this.fj_1[this.bj_1] = isOptional;
  this.dj_1[this.bj_1] = null;
  if (this.bj_1 === (this.aj_1 - 1 | 0)) {
    this.gj_1 = buildIndices(this);
  }
};
protoOf(PluginGeneratedSerialDescriptor).kj = function (name, isOptional, $super) {
  isOptional = isOptional === VOID ? false : isOptional;
  var tmp;
  if ($super === VOID) {
    this.bk(name, isOptional);
    tmp = Unit_instance;
  } else {
    tmp = $super.bk.call(this, name, isOptional);
  }
  return tmp;
};
protoOf(PluginGeneratedSerialDescriptor).af = function (index) {
  return getChecked(_get_childSerializers__7vnyfa(this), index).md();
};
protoOf(PluginGeneratedSerialDescriptor).bf = function (index) {
  return getChecked_0(this.fj_1, index);
};
protoOf(PluginGeneratedSerialDescriptor).ze = function (index) {
  var tmp0_elvis_lhs = getChecked(this.dj_1, index);
  return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
};
protoOf(PluginGeneratedSerialDescriptor).xe = function (index) {
  return getChecked(this.cj_1, index);
};
protoOf(PluginGeneratedSerialDescriptor).ye = function (name) {
  var tmp0_elvis_lhs = this.gj_1.t1(name);
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
    if (!(this.ie() === other.ie())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!contentEquals(this.ak(), other.ak())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    if (!(this.ve() === other.ve())) {
      tmp$ret$0 = false;
      break $l$block_5;
    }
    var inductionVariable = 0;
    var last = this.ve();
    if (inductionVariable < last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(this.af(index).ie() === other.af(index).ie())) {
          tmp$ret$0 = false;
          break $l$block_5;
        }
        if (!equals(this.af(index).te(), other.af(index).te())) {
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
    return $this_toStringImpl.xe(i) + ': ' + $this_toStringImpl.af(i).ie();
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
  this.vk_1 = new PrimitiveSerialDescriptor('kotlin.String', STRING_getInstance());
}
protoOf(StringSerializer).md = function () {
  return this.vk_1;
};
protoOf(StringSerializer).wk = function (encoder, value) {
  return encoder.zg(value);
};
protoOf(StringSerializer).od = function (encoder, value) {
  return this.wk(encoder, (!(value == null) ? typeof value === 'string' : false) ? value : THROW_CCE());
};
protoOf(StringSerializer).nd = function (decoder) {
  return decoder.cg();
};
var StringSerializer_instance;
function StringSerializer_getInstance() {
  if (StringSerializer_instance == null)
    new StringSerializer();
  return StringSerializer_instance;
}
function error($this) {
  throw IllegalStateException_init_$Create$('Primitive descriptor ' + $this.xk_1 + ' does not have elements');
}
function PrimitiveSerialDescriptor(serialName, kind) {
  this.xk_1 = serialName;
  this.yk_1 = kind;
}
protoOf(PrimitiveSerialDescriptor).ie = function () {
  return this.xk_1;
};
protoOf(PrimitiveSerialDescriptor).te = function () {
  return this.yk_1;
};
protoOf(PrimitiveSerialDescriptor).ve = function () {
  return 0;
};
protoOf(PrimitiveSerialDescriptor).xe = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).ye = function (name) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).bf = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).af = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).ze = function (index) {
  error(this);
};
protoOf(PrimitiveSerialDescriptor).toString = function () {
  return 'PrimitiveDescriptor(' + this.xk_1 + ')';
};
protoOf(PrimitiveSerialDescriptor).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof PrimitiveSerialDescriptor))
    return false;
  if (this.xk_1 === other.xk_1 && equals(this.yk_1, other.yk_1))
    return true;
  return false;
};
protoOf(PrimitiveSerialDescriptor).hashCode = function () {
  return getStringHashCode(this.xk_1) + imul(31, this.yk_1.hashCode()) | 0;
};
function IntSerializer() {
  IntSerializer_instance = this;
  this.zk_1 = new PrimitiveSerialDescriptor('kotlin.Int', INT_getInstance());
}
protoOf(IntSerializer).md = function () {
  return this.zk_1;
};
protoOf(IntSerializer).al = function (encoder, value) {
  return encoder.wg(value);
};
protoOf(IntSerializer).od = function (encoder, value) {
  return this.al(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
};
protoOf(IntSerializer).nd = function (decoder) {
  return decoder.zf();
};
var IntSerializer_instance;
function IntSerializer_getInstance() {
  if (IntSerializer_instance == null)
    new IntSerializer();
  return IntSerializer_instance;
}
function LongSerializer() {
  LongSerializer_instance = this;
  this.bl_1 = new PrimitiveSerialDescriptor('kotlin.Long', LONG_getInstance());
}
protoOf(LongSerializer).md = function () {
  return this.bl_1;
};
protoOf(LongSerializer).cl = function (encoder, value) {
  return encoder.xg(value);
};
protoOf(LongSerializer).od = function (encoder, value) {
  return this.cl(encoder, value instanceof Long ? value : THROW_CCE());
};
protoOf(LongSerializer).nd = function (decoder) {
  return decoder.ag();
};
var LongSerializer_instance;
function LongSerializer_getInstance() {
  if (LongSerializer_instance == null)
    new LongSerializer();
  return LongSerializer_instance;
}
function DoubleSerializer() {
  DoubleSerializer_instance = this;
  this.dl_1 = new PrimitiveSerialDescriptor('kotlin.Double', DOUBLE_getInstance());
}
protoOf(DoubleSerializer).md = function () {
  return this.dl_1;
};
protoOf(DoubleSerializer).el = function (encoder, value) {
  return encoder.yg(value);
};
protoOf(DoubleSerializer).od = function (encoder, value) {
  return this.el(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
};
protoOf(DoubleSerializer).nd = function (decoder) {
  return decoder.bg();
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
protoOf(NamedValueDecoder).hl = function (_this__u8e3s4, index) {
  return this.jl(this.il(_this__u8e3s4, index));
};
protoOf(NamedValueDecoder).jl = function (nestedName) {
  var tmp0_elvis_lhs = this.ml();
  return this.nl(tmp0_elvis_lhs == null ? '' : tmp0_elvis_lhs, nestedName);
};
protoOf(NamedValueDecoder).il = function (descriptor, index) {
  return descriptor.xe(index);
};
protoOf(NamedValueDecoder).nl = function (parentName, childName) {
  var tmp;
  // Inline function 'kotlin.text.isEmpty' call
  if (charSequenceLength(parentName) === 0) {
    tmp = childName;
  } else {
    tmp = parentName + '.' + childName;
  }
  return tmp;
};
protoOf(NamedValueDecoder).ol = function () {
  return this.kl_1.m() ? '$' : joinToString(this.kl_1, '.', '$.');
};
function tagBlock($this, tag, block) {
  $this.wl(tag);
  var r = block();
  if (!$this.ll_1) {
    $this.xl();
  }
  $this.ll_1 = false;
  return r;
}
function TaggedDecoder$decodeSerializableElement$lambda(this$0, $deserializer, $previousValue) {
  return function () {
    return this$0.eg($deserializer, $previousValue);
  };
}
function TaggedDecoder$decodeNullableSerializableElement$lambda(this$0, $deserializer, $previousValue) {
  return function () {
    var tmp0 = this$0;
    // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
    var isNullabilitySupported = $deserializer.md().ne();
    var tmp;
    if (isNullabilitySupported || tmp0.xf()) {
      tmp = this$0.eg($deserializer, $previousValue);
    } else {
      tmp = tmp0.yf();
    }
    return tmp;
  };
}
function TaggedDecoder() {
  var tmp = this;
  // Inline function 'kotlin.collections.arrayListOf' call
  tmp.kl_1 = ArrayList_init_$Create$();
  this.ll_1 = false;
}
protoOf(TaggedDecoder).pg = function () {
  return EmptySerializersModule_0();
};
protoOf(TaggedDecoder).pl = function (tag) {
  throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
};
protoOf(TaggedDecoder).ql = function (tag) {
  return true;
};
protoOf(TaggedDecoder).rl = function (tag) {
  var tmp = this.pl(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).sl = function (tag) {
  var tmp = this.pl(tag);
  return tmp instanceof Long ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).tl = function (tag) {
  var tmp = this.pl(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).ul = function (tag) {
  var tmp = this.pl(tag);
  return typeof tmp === 'string' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).vl = function (tag, enumDescriptor) {
  var tmp = this.pl(tag);
  return typeof tmp === 'number' ? tmp : THROW_CCE();
};
protoOf(TaggedDecoder).eg = function (deserializer, previousValue) {
  return this.fg(deserializer);
};
protoOf(TaggedDecoder).xf = function () {
  var tmp0_elvis_lhs = this.ml();
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return false;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var currentTag = tmp;
  return this.ql(currentTag);
};
protoOf(TaggedDecoder).yf = function () {
  return null;
};
protoOf(TaggedDecoder).zf = function () {
  return this.rl(this.xl());
};
protoOf(TaggedDecoder).ag = function () {
  return this.sl(this.xl());
};
protoOf(TaggedDecoder).bg = function () {
  return this.tl(this.xl());
};
protoOf(TaggedDecoder).cg = function () {
  return this.ul(this.xl());
};
protoOf(TaggedDecoder).dg = function (enumDescriptor) {
  return this.vl(this.xl(), enumDescriptor);
};
protoOf(TaggedDecoder).gg = function (descriptor) {
  return this;
};
protoOf(TaggedDecoder).hg = function (descriptor) {
};
protoOf(TaggedDecoder).ig = function (descriptor, index) {
  return this.rl(this.hl(descriptor, index));
};
protoOf(TaggedDecoder).jg = function (descriptor, index) {
  return this.sl(this.hl(descriptor, index));
};
protoOf(TaggedDecoder).kg = function (descriptor, index) {
  return this.tl(this.hl(descriptor, index));
};
protoOf(TaggedDecoder).lg = function (descriptor, index) {
  return this.ul(this.hl(descriptor, index));
};
protoOf(TaggedDecoder).mg = function (descriptor, index, deserializer, previousValue) {
  var tmp = this.hl(descriptor, index);
  return tagBlock(this, tmp, TaggedDecoder$decodeSerializableElement$lambda(this, deserializer, previousValue));
};
protoOf(TaggedDecoder).og = function (descriptor, index, deserializer, previousValue) {
  var tmp = this.hl(descriptor, index);
  return tagBlock(this, tmp, TaggedDecoder$decodeNullableSerializableElement$lambda(this, deserializer, previousValue));
};
protoOf(TaggedDecoder).ml = function () {
  return lastOrNull(this.kl_1);
};
protoOf(TaggedDecoder).wl = function (name) {
  this.kl_1.i(name);
};
protoOf(TaggedDecoder).xl = function () {
  var r = this.kl_1.w2(get_lastIndex_0(this.kl_1));
  this.ll_1 = true;
  return r;
};
function get_EmptySerializersModuleLegacyJs() {
  _init_properties_SerializersModule_kt__u78ha3();
  return EmptySerializersModule;
}
var EmptySerializersModule;
function SerializersModule() {
}
protoOf(SerializersModule).oe = function (kClass, typeArgumentsSerializers, $super) {
  typeArgumentsSerializers = typeArgumentsSerializers === VOID ? emptyList() : typeArgumentsSerializers;
  return $super === VOID ? this.yl(kClass, typeArgumentsSerializers) : $super.yl.call(this, kClass, typeArgumentsSerializers);
};
function SerialModuleImpl(class2ContextualFactory, polyBase2Serializers, polyBase2DefaultSerializerProvider, polyBase2NamedSerializers, polyBase2DefaultDeserializerProvider, hasInterfaceContextualSerializers) {
  SerializersModule.call(this);
  this.zl_1 = class2ContextualFactory;
  this.am_1 = polyBase2Serializers;
  this.bm_1 = polyBase2DefaultSerializerProvider;
  this.cm_1 = polyBase2NamedSerializers;
  this.dm_1 = polyBase2DefaultDeserializerProvider;
  this.em_1 = hasInterfaceContextualSerializers;
}
protoOf(SerialModuleImpl).nh = function (baseClass, value) {
  if (!baseClass.h8(value))
    return null;
  var tmp0_safe_receiver = this.am_1.t1(baseClass);
  var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.t1(getKClassFromExpression(value));
  var registered = (!(tmp == null) ? isInterface(tmp, SerializationStrategy) : false) ? tmp : null;
  if (!(registered == null))
    return registered;
  var tmp_0 = this.bm_1.t1(baseClass);
  var tmp1_safe_receiver = (!(tmp_0 == null) ? typeof tmp_0 === 'function' : false) ? tmp_0 : null;
  return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(value);
};
protoOf(SerialModuleImpl).mh = function (baseClass, serializedClassName) {
  var tmp0_safe_receiver = this.cm_1.t1(baseClass);
  var tmp;
  if (tmp0_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlin.collections.get' call
    tmp = (isInterface(tmp0_safe_receiver, KtMap) ? tmp0_safe_receiver : THROW_CCE()).t1(serializedClassName);
  }
  var tmp_0 = tmp;
  var registered = (!(tmp_0 == null) ? isInterface(tmp_0, KSerializer) : false) ? tmp_0 : null;
  if (!(registered == null))
    return registered;
  var tmp_1 = this.dm_1.t1(baseClass);
  var tmp1_safe_receiver = (!(tmp_1 == null) ? typeof tmp_1 === 'function' : false) ? tmp_1 : null;
  return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(serializedClassName);
};
protoOf(SerialModuleImpl).yl = function (kClass, typeArgumentsSerializers) {
  var tmp0_safe_receiver = this.zl_1.t1(kClass);
  var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.fm(typeArgumentsSerializers);
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
protoOf(SerialDescriptorImpl).ne = get_isNullable;
protoOf(SerialDescriptorImpl).ue = get_isInline;
protoOf(AbstractDecoder).ng = decodeSerializableElement$default;
protoOf(AbstractDecoder).fg = decodeSerializableValue;
protoOf(AbstractDecoder).qg = decodeSequentially;
protoOf(AbstractDecoder).sg = decodeCollectionSize;
protoOf(AbstractEncoder).jh = encodeNotNullMark;
protoOf(AbstractEncoder).kh = beginCollection;
protoOf(AbstractEncoder).gh = encodeSerializableValue;
protoOf(AbstractEncoder).ih = encodeNullableSerializableValue;
protoOf(AbstractEncoder).lh = shouldEncodeElementDefault;
protoOf(ListLikeDescriptor).ne = get_isNullable;
protoOf(ListLikeDescriptor).ue = get_isInline;
protoOf(ListLikeDescriptor).we = get_annotations;
protoOf(PluginGeneratedSerialDescriptor).ne = get_isNullable;
protoOf(PluginGeneratedSerialDescriptor).ue = get_isInline;
protoOf(InlinePrimitiveDescriptor$1).rk = typeParametersSerializers;
protoOf(PrimitiveSerialDescriptor).ne = get_isNullable;
protoOf(PrimitiveSerialDescriptor).ue = get_isInline;
protoOf(PrimitiveSerialDescriptor).we = get_annotations;
protoOf(TaggedDecoder).ng = decodeSerializableElement$default;
protoOf(TaggedDecoder).fg = decodeSerializableValue;
protoOf(TaggedDecoder).qg = decodeSequentially;
protoOf(TaggedDecoder).sg = decodeCollectionSize;
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

