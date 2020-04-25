package com.codethen.mongo.adapter.generator;

import com.codethen.mongo.adapter.generator.sample.Address;
import com.codethen.mongo.adapter.generator.sample.AddressExt;
import com.codethen.mongo.adapter.generator.sample.Person;
import com.codethen.mongo.adapter.generator.sample.adapter.AddressAdapter;
import com.codethen.mongo.adapter.generator.sample.adapter.AddressExtAdapter;
import com.codethen.mongo.adapter.generator.sample.adapter.PersonAdapter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.mongodb.Function;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class AdapterGeneratorExample {

	public static void main(String[] args) throws Exception {

		generateAdapters();
		tryAdapters();
	}

	private static void generateAdapters() throws Exception {

		final String packageName = AdapterGeneratorExample.class.getPackage().getName() + ".sample.adapter";
		final String sourcePath = "src/main/java";

		final Map<Class<?>, TypeSpec> adapters = new HashMap<>();


		// This is a simple class

		final TypeSpec addressAdapter = new AdapterBuilder<>(Address.class, adapters, setupFields(m -> m
			.put("street", "str")
			.put("number", "num")
		)).build();

		JavaFile.builder(packageName, addressAdapter).build().writeTo(new File(sourcePath));
		adapters.put(Address.class, addressAdapter);

		// This is a subclass, so indicates the superclass adapter

		final TypeSpec addressExtAdapter = new AdapterBuilder<AddressExt>(AddressExt.class, adapters, setupFields(m -> m
			.put("city", "city")
		)){
			@Override
			public Class<? extends BaseDocumentAdapter> getAdapterSuperclass() {
				return AddressAdapter.class;
			}
		}.build();

		JavaFile.builder(packageName, addressExtAdapter).build().writeTo(new File(sourcePath));
		adapters.put(AddressExt.class, addressExtAdapter);


		// This is a more complex class, including fields of basic and special types like ObjectId, Enum and List (of all the previous types)

		final PersonAdapter.Fields f = PersonAdapter.fields; // For convenience, you can use an alias

		final TypeSpec personAdapter = new AdapterBuilder<Person>(Person.class, adapters, setupFields(m -> m
			.put("id", f.id) // We can use the field after we have generated the PersonAdapter :)
			.put("friendIds", "friends")
			.put("name", "name")
			.put("nicknames", "nicks")
			.put("age", "age")
			.put("number", "num")
			.put("famous", "fam")
			.put("gender", "gen")
			.put("preferredGenders", "prefs")
			.put("address", "adr")
			.put("otherAddresses", "other")
		)){
			@Override
			public Collection<String> objectIdDocFields() {
				return Arrays.asList(f.id, f.friendIds);
			}

			// The next methods can be overridden to configure custom mappings.
			// In this example, we store a String field number as an Integer into the database.

			@Override
			public Object buildModelFieldExtractor(String modelVar, Field modelField) {

				final Object result = super.buildModelFieldExtractor(modelVar, modelField);

				// Example of custom mapping: convert from String to Integer
				if (modelField.getName().equals("number"))
					return "Integer.parseInt(" + result + ")";

				return result;
			}

			@Override
			public Object buildDocFieldExtractor(String docVar, Field modelField, Type fieldType) {

				// Example of custom mapping: convert from Integer to String
				if (modelField.getName().equals("number"))
					return CodeBlock.builder().add("String.valueOf($L)", super.buildDocFieldExtractor(docVar, modelField, Object.class)).build();

				return super.buildDocFieldExtractor(docVar, modelField, fieldType);
			}
		}.build();

		JavaFile.builder(packageName, personAdapter).build().writeTo(new File(sourcePath));
		adapters.put(Person.class, personAdapter);

	}

	private static void tryAdapters() {

		final Function<Document, Person> doc2model = PersonAdapter.INSTANCE::doc2model;
		final Function<Person, Document> model2doc = PersonAdapter.INSTANCE::model2doc;

		final MongoDatabase db = getMongoDatabase("adapter" + "generator" + "example");
		final MongoCollection<Document> people = db.getCollection("people");

		people.drop();
		people.insertOne(model2doc.apply(createSamplePerson(p -> { p.setName("P1"); p.getAddress().setStreet("S1"); })));
		people.insertOne(model2doc.apply(createSamplePerson(p -> { p.setName("P2"); p.getAddress().setStreet("S2"); })));
		people.insertOne(model2doc.apply(createSamplePerson(p -> { p.setName("P3"); p.getAddress().setStreet("S3"); })));

		// For convenience, you can use an alias
		final PersonAdapter.Fields pf = PersonAdapter.fields;
		final AddressExtAdapter.Fields af = AddressExtAdapter.fields;

		// Note that street is a field inherited from Address, but it's also available in AddressExtAdapter

		final List<Person> peopleFound = people
			.find(queryBy(pf.address + "." + af.street, "S2"))
			.map(doc2model)
			.into(new ArrayList<>());

		peopleFound.forEach(p -> printPerson(p));

		if (!peopleFound.stream().map(p -> p.getName()).collect(toList()).equals(singletonList("P2"))) {
			throw new IllegalStateException("Data found not as expected");
		}
	}

	private static Document queryBy(String key, Object value) {
		return new Document(key, value);
	}

	/**
	 * http://mongodb.github.io/mongo-java-driver/4.0/driver/getting-started/quick-start-pojo/
	 */
	private static void tryCodecRegistry() {

		final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
			fromProviders(PojoCodecProvider.builder().automatic(true).build()));

		final MongoDatabase db = getMongoDatabase("test").withCodecRegistry(pojoCodecRegistry);
		final MongoCollection<Person> people = db.getCollection("ppl", Person.class);

		final Person person = createSamplePerson(p -> {});

		people.insertOne(person); // PersonAdapter.INSTANCE.model2doc(person)

		people.find().forEach((Consumer<? super Person>) p -> { // map(doc2model)
			printPerson(p);
		});
	}

	private static MongoDatabase getMongoDatabase(String dbName) {
		final MongoClient mongoClient = new MongoClient();
		return mongoClient.getDatabase(dbName);
	}

	private static Person createSamplePerson(Consumer<Person> config) {

		final AddressExt address = new AddressExt();
		address.setStreet("Alaba");
		address.setNumber(61);
		address.setCity("Barcelona");

		final Person person = new Person();
		person.setName("John");
		person.setFriendIds(Arrays.asList(new ObjectId().toString(), new ObjectId().toString()));
		person.setAddress(address);
		person.setAge(20);
		person.setNumber("123");
		person.setFamous(true);
		person.setGender(Person.Gender.MALE);
		person.setPreferredGenders(Arrays.asList(Person.Gender.MALE, Person.Gender.FEMALE));
		person.setNicknames(Arrays.asList("Joni", "Gin"));
		person.setOtherAddresses(Arrays.asList(address, address));

		config.accept(person);

		return person;
	}


	private static void printPerson(Person p) {
		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(p));
	}

	private static Map<String, String> setupFields(Consumer<ImmutableMap.Builder<String, String>> config) {

		final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		config.accept(builder);
		return builder.build();
	}
}
