package com.codethen.mongo.adapter.generator;

import com.codethen.mongo.adapter.generator.sample.Address;
import com.codethen.mongo.adapter.generator.sample.AddressExt;
import com.codethen.mongo.adapter.generator.sample.Person;
import com.codethen.mongo.adapter.generator.sample.adapter.AddressAdapter;
import com.codethen.mongo.adapter.generator.sample.adapter.AddressExtAdapter;
import com.codethen.mongo.adapter.generator.sample.adapter.PersonAdapter;
import com.codethen.util.MapBuilder;
import com.google.gson.GsonBuilder;
import com.mongodb.Function;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.squareup.javapoet.CodeBlock;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

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

		final AdapterBuilderContext context = new AdapterBuilderContext(sourcePath, packageName);

		/** {@link Address} is a simple class. We just define the fields. */
		context.createAdapter(new AdapterBuilder<>(Address.class, fields(m -> m
			.put("street", "str")
			.put("number", "num")
		)));

		/**
		 * {@link AddressExt} is a subclass, so we indicate the superclass adapter.
		 * Note that the {@link AddressAdapter} must be already generated.
		 */
		context.createAdapter(new AdapterBuilder<>(AddressExt.class, AddressAdapter.class, fields(m -> m
			.put("city", "city")
		)));

		// Note that we can use this object to refer to fields (since the adapter is already generated)
		final PersonAdapter.Fields f = PersonAdapter.fields;

		/**
		 * {@link Person} is a complex class, including fields of:
		 * - Basic types like {@link String}, {@link Integer}, {@link Boolean}
		 * - Special types like {@link ObjectId}, {@link Enum}
		 * - Fields of other model classes like {@link Address} or {@link AddressExt}
		 * - List fields (of all the previous types: basic, ObjectId, Enum)
		 */
		context.createAdapter(new AdapterBuilder<Person>(Person.class, fields(m -> m
			.put("id", f.id)
			.put("friendIds", f.friendIds)
			.put("name", f.name)
			.put("nicknames", "nicks")
			.put("age", "age")
			.put("number", "num")
			.put("famous", "fam")
			.put("gender", "gen")
			.put("preferredGenders", "prefs")
			.put("address", "adr")
			.put("otherAddresses", "other")
		)){
			/**
			 * Here we indicate the fields that should be persisted as {@link ObjectId}s.
			 * By default, it's just the "_id" field.
			 * Note that we can refer to fields that are {@link List} of {@link String}.
			 */
			@Override
			public Collection<String> objectIdDocFields() {
				return Arrays.asList(f.id, f.friendIds);
			}

			// The next methods show a custom mapping. This should be rarely used.
			// This is a weird example, just to show how you could write your custom mappings.
			// We store a String field number as an Integer into the database.

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
		});
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

	private static Map<String, String> fields(Function<MapBuilder<String, String>, MapBuilder<String, String>> config) {

		return config.apply(MapBuilder.linked()).build();
	}
}
