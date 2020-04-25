package com.codethen.mongo.adapter.generator.sample.adapter;

import com.codethen.mongo.adapter.generator.BaseDocumentAdapter;
import com.codethen.mongo.adapter.generator.sample.Address;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import org.bson.Document;

@SuppressWarnings("unchecked")
public class AddressAdapter<T extends Address> extends BaseDocumentAdapter<T> {
  public static final Fields fields = new Fields();

  public static final AddressAdapter<Address> INSTANCE = new AddressAdapter<>();

  @Override
  public T newModelInstance() {
    return (T) new Address();
  }

  @Override
  public Document model2doc(T model) {
    final Document doc = super.model2doc(model);
    if (doc == null) return null;
    appendTo(doc, fields.street, model.getStreet());
    appendTo(doc, fields.number, model.getNumber());
    return doc;
  }

  @Override
  public T doc2model(Document doc) {
    final T model = super.doc2model(doc);
    if (model == null) return null;
    model.setStreet((String) doc.get(fields.street));
    model.setNumber((int) doc.get(fields.number));
    return model;
  }

  public static class Fields extends BaseDocumentAdapter.Fields {
    public final String street = "str";

    public final String number = "num";
  }
}
