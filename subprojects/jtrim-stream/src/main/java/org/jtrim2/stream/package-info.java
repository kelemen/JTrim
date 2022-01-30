/**
 * Contains classes and interfaces for synchronous stream processing. This API
 * was not designed to supplement Java's {@code Stream} API, but rather replaces
 * it in many circumstances, or provides feature not easily feasible there.
 * <P>
 * The main goal of this API is to allow developers to create simple
 * implementations producing and consuming streams. Many common implementations
 * are already present in this package.
 * <P>
 * Notable implementations are collecting elements of a stream into batches or
 * easily allowing producers and consumers to run concurrently yet keeping
 * producer and consumer implementations simple. See the following complex
 * example for an example usage:
 * <pre>{@code
 * void runFileMergeDemo(Path dir) throws Exception {
 *   newLineReaderProducer(dir.resolve("demo-src1.txt"))
 *       .toFluent()
 *       .concat(newLineReaderProducer(dir.resolve("demo-src2.txt")))
 *       .concat(newLineReaderProducer(dir.resolve("demo-src3.txt")))
 *       .batch(1000)
 *       .toBackground("mapper-executor", 0)
 *       .apply(SeqProducer::flatteningProducer)
 *       .map(exampleStringMapper())
 *       .batch(1000)
 *       .toBackground("writer-executor", 0)
 *       .apply(SeqProducer::flatteningProducer)
 *       .withConsumer(
 *           newLineWriterConsumer(dir.resolve("demo-dest.txt"))
 *           .toFluent()
 *           .<String>thenContextFree(line -> System.out.println("Log: " + line))
 *           .unwrap()
 *       )
 *       .execute(Cancellation.UNCANCELABLE_TOKEN);
 * }
 *
 * SeqProducer<String> newLineReaderProducer(Path srcFile) {
 *   return (cancelToken, consumer) -> {
 *     try (BufferedReader source = Files.newBufferedReader(srcFile, StandardCharsets.UTF_8)) {
 *       String line;
 *       while ((line = source.readLine()) != null) {
 *         cancelToken.checkCanceled();
 *         consumer.processElement(line);
 *       }
 *     }
 *   };
 * }
 *
 * SeqConsumer<Object> newLineWriterConsumer(Path destFile) {
 *   return (cancelToken, producer) -> {
 *     try (Writer writer = Files.newBufferedWriter(destFile, StandardCharsets.UTF_8)) {
 *       producer.transferAll(cancelToken, line -> {
 *         cancelToken.checkCanceled();
 *         writer.write(line + "\n");
 *       });
 *     }
 *   };
 * }
 *
 * SeqMapper<String, String> exampleStringMapper() {
 *   return (cancelToken, producer, consumerDef) -> {
 *     consumerDef.consumeAll(cancelToken, (producerCancelToken, consumer) -> {
 *       producer.transferAll(producerCancelToken, line -> {
 *         producerCancelToken.checkCanceled();
 *         consumer.processElement("mapper(" + line + ")");
 *       });
 *     });
 *   };
 * }
 * }</pre>
 * The above example reads the lines of 3 files into a single sequence maps the lines,
 * then writes the mapped lines out to a single file while logging each line being persisted
 * as an independent action. Aside from this, the above setup allows the reading and writing
 * to run concurrently, and also batches lines to reduce overhead.
 *
 * @see org.jtrim2.stream.SeqProducer
 * @see org.jtrim2.stream.SeqGroupProducer
 *
 * @see org.jtrim2.stream.SeqConsumer
 * @see org.jtrim2.stream.SeqGroupConsumer
 *
 * @see org.jtrim2.stream.SeqMapper
 * @see org.jtrim2.stream.SeqGroupMapper
 *
 * @see org.jtrim2.stream.AsyncProducers
 */
package org.jtrim2.stream;
