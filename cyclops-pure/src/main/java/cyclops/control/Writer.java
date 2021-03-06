package cyclops.control;

import com.oath.cyclops.hkt.Higher;
import com.oath.cyclops.hkt.Higher2;
import com.oath.cyclops.types.functor.Transformable;
import cyclops.function.Function3;
import cyclops.function.Function4;
import cyclops.function.Monoid;
import com.oath.cyclops.hkt.DataWitness.writer;
import cyclops.typeclasses.functor.Functor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import cyclops.data.tuple.Tuple;
import cyclops.data.tuple.Tuple2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

@AllArgsConstructor(access= AccessLevel.PRIVATE)
@Getter
public final class Writer<W, T> implements Transformable<T>, Iterable<T>,Higher2<writer,W,T>, Serializable {
    private static final long serialVersionUID = 1L;

    private final Tuple2<T,W> value;
    private final Monoid<W> monoid;

    public <R> Writer<W, R> map(Function<? super T,? extends  R> mapper) {
        return writer(mapper.apply(value._1()), value._2(), monoid);
    }

    public <R> R fold(BiFunction<? super Tuple2<T,W>,? super Monoid<W>,? extends R> fn){
        return fn.apply(value,monoid);
    }
    public <R> Writer<W, R> flatMap(Function<? super T,? extends  Writer<W, ? extends R>> fn) {
        Writer<W, ? extends R> writer = fn.apply(value._1());
        return writer(writer.value._1(), writer.monoid.apply(value._2(), writer.value._2()), writer.monoid);
    }

    public Writer<W,T> tell(W write){
        return writer(value._1(),monoid.apply(write,value._2()),monoid);
    }

    public <R> Writer<W,R> set(R value){
            return writer(value,this.value._2(),monoid);
    }

    /*
     * Perform a For Comprehension over a Writer, accepting 3 generating function.
             * This results in a four level nested internal iteration over the provided Writers.
      *
              *  <pre>
      * {@code
      *
      *   import static com.oath.cyclops.reactor.Writers.forEach4;
      *
         forEach4(Writer.just(1),
                 a-> Writer.just(a+1),
                 (a,b) -> Writer.<Integer>just(a+b),
                 a                  (a,b,c) -> Writer.<Integer>just(a+b+c),
                 Tuple::tuple)
      *
      * }
      * </pre>
             *
             * @param value1 top level Writer
      * @param value2 Nested Writer
      * @param value3 Nested Writer
      * @param value4 Nested Writer
      * @param yieldingFunction Generates a result per combination
      * @return Writer with a combined value generated by the yielding function
      */
    public  <R1, R2, R3, R4> Writer<W,R4> forEach4(Function<? super T, ? extends Writer<W,R1>> value2,
                                                   BiFunction<? super T, ? super R1, ? extends Writer<W,R2>> value3,
                                                   Function3<? super T, ? super R1, ? super R2, ? extends Writer<W,R3>> value4,
                                                   Function4<? super T, ? super R1, ? super R2, ? super R3, ? extends R4> yieldingFunction) {


        return this.flatMap(in -> {

            Writer<W,R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Writer<W,R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {

                    Writer<W,R3> c = value4.apply(in,ina,inb);

                    return c.map(in2 -> {

                        return yieldingFunction.apply(in, ina, inb, in2);

                    });

                });


            });


        });

    }



    /**
     * Perform a For Comprehension over a Writer, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Writers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Writers.forEach3;
     *
    forEach3(Writer.just(1),
    a-> Writer.just(a+1),
    (a,b) -> Writer.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value2 Nested Writer
     * @param value3 Nested Writer
     * @param yieldingFunction Generates a result per combination
     * @return Writer with a combined value generated by the yielding function
     */
    public <R1, R2, R4> Writer<W,R4> forEach3(Function<? super T, ? extends Writer<W,R1>> value2,
                                               BiFunction<? super T, ? super R1, ? extends Writer<W,R2>> value3,
                                               Function3<? super T, ? super R1, ? super R2, ? extends R4> yieldingFunction) {

        return this.flatMap(in -> {

            Writer<W,R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Writer<W,R2> b = value3.apply(in,ina);
                return b.map(in2 -> {
                    return yieldingFunction.apply(in, ina, in2);

                });



            });

        });

    }


    /**
     * Perform a For Comprehension over a Writer, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Writers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.oath.cyclops.reactor.Writers.forEach;
     *
    forEach(Writer.just(1),
    a-> Writer.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value2 Nested Writer
     * @param yieldingFunction Generates a result per combination
     * @return Writer with a combined value generated by the yielding function
     */
    public <R1, R4> Writer<W,R4> forEach2(Function<? super T, Writer<W,R1>> value2,
                                           BiFunction<? super T, ? super R1, ? extends R4> yieldingFunction) {

        return this.flatMap(in -> {

            Writer<W,R1> a = value2.apply(in);
            return a.map(in2 -> {
                return yieldingFunction.apply(in, in2);

            });

        });

    }

    public static <W, T> Writer<W, T> writer(T value, Monoid<W> combiner) {
        return new Writer<W,T>(Tuple.tuple(value, combiner.zero()), combiner);
    }
    public static <W, T> Writer<W, T> writer(T value, W initial, Monoid<W> combiner) {
        return new Writer<W,T>(Tuple.tuple(value, initial), combiner);
    }
    public static <W, T> Writer<W, T> writer(Tuple2<T,W> values, Monoid<W> combiner) {
        return new Writer<W,T>(values, combiner);
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(value._1()).iterator();
    }


    public static <W,T> Writer<W,T> narrowK2(final Higher2<writer, W,T> t) {
        return (Writer<W,T>)t;
    }
    public static <W,T> Writer<W,T> narrowK(final Higher<Higher<writer, W>,T> t) {
        return (Writer)t;
    }
    public static <W,T> Higher<Higher<writer, W>, T> widen(Writer<W,T> narrow) {
    return narrow;
  }

}
