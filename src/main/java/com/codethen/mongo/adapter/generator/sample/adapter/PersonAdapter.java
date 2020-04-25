package com.codethen.mongo.adapter.generator.sample.adapter;

import com.codethen.mongo.adapter.generator.BaseDocumentAdapter;
import com.codethen.mongo.adapter.generator.sample.Person;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

@SuppressWarnings("unchecked")
public class PersonAdapter<T extends Person> extends BaseDocumentAdapter<T> {
  public static final Fields fields = new Fields();

  public static final PersonAdapter<Person> INSTANCE = new PersonAdapter<>();

  @Override
  public T newModelInstance() {
    return (T) new Person();
  }

  @Override
  public Document model2doc(T model) {
    final Document doc = super.model2doc(model);
    if (doc == null) return null;
    appendTo(doc, fields.id, string2id(model.getId()));
    appendTo(doc, fields.friendIds, string2id(model.getFriendIds()));
    appendTo(doc, fields.name, model.getName());
    appendTo(doc, fields.nicknames, model.getNicknames());
    appendTo(doc, fields.age, model.getAge());
    appendTo(doc, fields.number, Integer.parseInt(model.getNumber()));
    appendTo(doc, fields.famous, model.isFamous());
    appendTo(doc, fields.gender, enum2obj(model.getGender()));
    appendTo(doc, fields.preferredGenders, enum2obj(model.getPreferredGenders()));
    appendTo(doc, fields.address, AddressExtAdapter.INSTANCE.model2doc(model.getAddress()));
    appendTo(doc, fields.otherAddresses, AddressAdapter.INSTANCE.model2doc(model.getOtherAddresses()));
    return doc;
  }

  @Override
  public T doc2model(Document doc) {
    final T model = super.doc2model(doc);
    if (model == null) return null;
    model.setId(id2string((ObjectId) doc.get(fields.id)));
    model.setFriendIds(id2string((List<ObjectId>) doc.get(fields.friendIds)));
    model.setName((String) doc.get(fields.name));
    model.setNicknames((List<String>) doc.get(fields.nicknames));
    model.setAge((int) doc.get(fields.age));
    model.setNumber(String.valueOf(doc.get(fields.number)));
    model.setFamous((boolean) doc.get(fields.famous));
    model.setGender(obj2enum(doc.get(fields.gender), Person.Gender.class));
    model.setPreferredGenders(obj2enum((List<Object>) doc.get(fields.preferredGenders), Person.Gender.class));
    model.setAddress(AddressExtAdapter.INSTANCE.doc2model((Document) doc.get(fields.address)));
    model.setOtherAddresses(AddressAdapter.INSTANCE.doc2model((List<Document>) doc.get(fields.otherAddresses)));
    return model;
  }

  public static class Fields extends BaseDocumentAdapter.Fields {
    public final String id = "_id";

    public final String friendIds = "friends";

    public final String name = "name";

    public final String nicknames = "nicks";

    public final String age = "age";

    public final String number = "num";

    public final String famous = "fam";

    public final String gender = "gen";

    public final String preferredGenders = "prefs";

    public final String address = "adr";

    public final String otherAddresses = "other";
  }
}
