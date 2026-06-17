package dev.engine_room.flywheel.backend.compile.component;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.ArrayElementType;
import dev.engine_room.flywheel.api.layout.ElementType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.MatrixElementType;
import dev.engine_room.flywheel.api.layout.ScalarElementType;
import dev.engine_room.flywheel.api.layout.UnsignedIntegerRepr;
import dev.engine_room.flywheel.api.layout.ValueRepr;
import dev.engine_room.flywheel.api.layout.VectorElementType;
import dev.engine_room.flywheel.backend.compile.LayoutInterpreter;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslExpr;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

public abstract class InstanceAssemblerComponent implements SourceComponent {
   protected static final String STRUCT_NAME = "FlwInstance";
   protected static final String UNPACK_FN_NAME = "_flw_unpackInstance";
   protected static final String UNPACK_ARG = "index";
   private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
   private static final EnumMap<IntegerRepr, Function<GlslExpr, GlslExpr>> INT_UNPACKING_FUNCS = new EnumMap<>(IntegerRepr.class);
   private static final EnumMap<UnsignedIntegerRepr, Function<GlslExpr, GlslExpr>> UINT_UNPACKING_FUNCS = new EnumMap<>(UnsignedIntegerRepr.class);
   private static final EnumMap<FloatRepr, Function<GlslExpr, GlslExpr>> FLOAT_UNPACKING_FUNCS = new EnumMap<>(FloatRepr.class);
   protected final Layout layout;

   public InstanceAssemblerComponent(InstanceType<?> type) {
      this.layout = type.layout();
   }

   private static GlslExpr signExtendByte(GlslExpr e) {
      return e.xor(128).sub(128);
   }

   private static GlslExpr signExtendShort(GlslExpr e) {
      return e.xor(32768).sub(32768);
   }

   @Override
   public Collection<? extends SourceComponent> included() {
      return Collections.emptyList();
   }

   @Override
   public String source() {
      GlslBuilder builder = new GlslBuilder();
      this.generateUnpacking(builder);
      builder.blankLine();
      return builder.build();
   }

   protected abstract void generateUnpacking(GlslBuilder var1);

   protected abstract GlslExpr access(int var1);

   protected GlslExpr unpackElement(Layout.Element element) {
      return this.unpackElement(element.type(), element.byteOffset());
   }

   private GlslExpr unpackElement(ElementType type, int byteOffset) {
      if (type instanceof ScalarElementType scalar) {
         return this.unpackScalar(scalar, byteOffset);
      } else if (type instanceof VectorElementType vector) {
         return this.unpackVector(vector, byteOffset);
      } else if (type instanceof MatrixElementType matrix) {
         return this.unpackMatrix(matrix, byteOffset);
      } else if (type instanceof ArrayElementType array) {
         return this.unpackArray(array, byteOffset);
      } else {
         throw new IllegalArgumentException("Unknown type " + type);
      }
   }

   private GlslExpr unpackScalar(ScalarElementType type, int byteOffset) {
      ValueRepr repr = type.repr();
      Function<GlslExpr, GlslExpr> unpackingFunc = getUnpackingFunc(repr);
      return this.unpackScalar(byteOffset, repr.byteSize(), unpackingFunc);
   }

   private GlslExpr unpackVector(VectorElementType type, int byteOffset) {
      ValueRepr repr = type.repr();
      int size = type.size();
      Function<GlslExpr, GlslExpr> unpackingFunc = getUnpackingFunc(repr);
      String outType = LayoutInterpreter.vectorTypeName(type);
      return this.unpackVector(outType, size, byteOffset, repr.byteSize(), unpackingFunc);
   }

   private GlslExpr unpackMatrix(MatrixElementType type, int byteOffset) {
      FloatRepr repr = type.repr();
      int rows = type.rows();
      int columns = type.columns();
      Function<GlslExpr, GlslExpr> unpackingFunc = FLOAT_UNPACKING_FUNCS.get(repr);
      String outType = LayoutInterpreter.matrixTypeName(type);
      int size = rows * columns;
      return this.unpackVector(outType, size, byteOffset, repr.byteSize(), unpackingFunc);
   }

   private GlslExpr unpackArray(ArrayElementType type, int byteOffset) {
      ElementType innerType = type.innerType();
      int innerByteSize = innerType.byteSize();
      int length = type.length();
      String outType = LayoutInterpreter.arrayTypeName(type);
      List<GlslExpr> args = new ArrayList<>();

      for (int i = 0; i < length; i++) {
         args.add(this.unpackElement(innerType, byteOffset + i * innerByteSize));
      }

      return GlslExpr.call(outType, args);
   }

   private GlslExpr unpackScalar(int byteOffset, int byteSize, Function<GlslExpr, GlslExpr> unpackingFunc) {
      int offset = byteOffset / byteSize;
      if (byteSize == 1) {
         return this.unpackByteBackedScalar(offset, unpackingFunc);
      } else {
         return byteSize == 2 ? this.unpackShortBackedScalar(offset, unpackingFunc) : this.unpackIntBackedScalar(offset, unpackingFunc);
      }
   }

   private GlslExpr unpackByteBackedScalar(int byteOffset, Function<GlslExpr, GlslExpr> unpackingFunc) {
      int bitPos = byteOffset % 4 * 8;
      if (BIG_ENDIAN) {
         bitPos = 24 - bitPos;
      }

      int wordOffset = byteOffset / 4;
      GlslExpr prepared = this.access(wordOffset).rsh(bitPos).and(255);
      return unpackingFunc.apply(prepared);
   }

   private GlslExpr unpackShortBackedScalar(int shortOffset, Function<GlslExpr, GlslExpr> unpackingFunc) {
      int bitPos = shortOffset % 2 * 16;
      if (BIG_ENDIAN) {
         bitPos = 16 - bitPos;
      }

      int wordOffset = shortOffset / 2;
      GlslExpr prepared = this.access(wordOffset).rsh(bitPos).and(65535);
      return unpackingFunc.apply(prepared);
   }

   private GlslExpr unpackIntBackedScalar(int intOffset, Function<GlslExpr, GlslExpr> unpackingFunc) {
      return unpackingFunc.apply(this.access(intOffset));
   }

   private GlslExpr unpackVector(String outType, int size, int byteOffset, int byteSize, Function<GlslExpr, GlslExpr> unpackingFunc) {
      int offset = byteOffset / byteSize;
      if (byteSize == 1) {
         return this.unpackByteBackedVector(outType, size, offset, unpackingFunc);
      } else {
         return byteSize == 2
            ? this.unpackShortBackedVector(outType, size, offset, unpackingFunc)
            : this.unpackIntBackedVector(outType, size, offset, unpackingFunc);
      }
   }

   private GlslExpr unpackByteBackedVector(String outType, int size, int byteOffset, Function<GlslExpr, GlslExpr> unpackingFunc) {
      List<GlslExpr> args = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         args.add(this.unpackByteBackedScalar(byteOffset + i, unpackingFunc));
      }

      return GlslExpr.call(outType, args);
   }

   private GlslExpr unpackShortBackedVector(String outType, int size, int shortOffset, Function<GlslExpr, GlslExpr> unpackingFunc) {
      List<GlslExpr> args = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         args.add(this.unpackShortBackedScalar(shortOffset + i, unpackingFunc));
      }

      return GlslExpr.call(outType, args);
   }

   private GlslExpr unpackIntBackedVector(String outType, int size, int intOffset, Function<GlslExpr, GlslExpr> unpackingFunc) {
      List<GlslExpr> args = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         args.add(this.unpackIntBackedScalar(intOffset + i, unpackingFunc));
      }

      return GlslExpr.call(outType, args);
   }

   private static Function<GlslExpr, GlslExpr> getUnpackingFunc(ValueRepr repr) {
      if (repr instanceof IntegerRepr intRepr) {
         return INT_UNPACKING_FUNCS.get(intRepr);
      } else if (repr instanceof UnsignedIntegerRepr uintRepr) {
         return UINT_UNPACKING_FUNCS.get(uintRepr);
      } else if (repr instanceof FloatRepr floatRepr) {
         return FLOAT_UNPACKING_FUNCS.get(floatRepr);
      } else {
         throw new IllegalArgumentException("Unknown repr " + repr);
      }
   }

   static {
      INT_UNPACKING_FUNCS.put(IntegerRepr.BYTE, e -> signExtendByte(e).cast("int"));
      INT_UNPACKING_FUNCS.put(IntegerRepr.SHORT, e -> signExtendShort(e).cast("int"));
      INT_UNPACKING_FUNCS.put(IntegerRepr.INT, e -> e.cast("int"));
      UINT_UNPACKING_FUNCS.put(UnsignedIntegerRepr.UNSIGNED_BYTE, Function.identity());
      UINT_UNPACKING_FUNCS.put(UnsignedIntegerRepr.UNSIGNED_SHORT, Function.identity());
      UINT_UNPACKING_FUNCS.put(UnsignedIntegerRepr.UNSIGNED_INT, Function.identity());
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.BYTE, e -> signExtendByte(e).cast("int").cast("float"));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.NORMALIZED_BYTE, e -> signExtendByte(e).cast("int").cast("float").div(127.0F).clamp(-1.0F, 1.0F));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.UNSIGNED_BYTE, e -> e.cast("float"));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.NORMALIZED_UNSIGNED_BYTE, e -> e.cast("float").div(255.0F));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.SHORT, e -> signExtendShort(e).cast("int").cast("float"));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.NORMALIZED_SHORT, e -> signExtendShort(e).cast("int").cast("float").div(32767.0F).clamp(-1.0F, 1.0F));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.UNSIGNED_SHORT, e -> e.cast("float"));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.NORMALIZED_UNSIGNED_SHORT, e -> e.cast("float").div(65535.0F));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.INT, e -> e.cast("int").cast("float"));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.NORMALIZED_INT, e -> e.cast("int").cast("float").div(2.1474836E9F).clamp(-1.0F, 1.0F));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.UNSIGNED_INT, e -> e.cast("float"));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.NORMALIZED_UNSIGNED_INT, e -> e.cast("float").div(4.2949673E9F));
      FLOAT_UNPACKING_FUNCS.put(FloatRepr.FLOAT, e -> e.callFunction("uintBitsToFloat"));
   }
}
