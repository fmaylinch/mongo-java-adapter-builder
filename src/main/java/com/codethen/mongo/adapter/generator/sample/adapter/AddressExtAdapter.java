package com.codethen.mongo.adapter.generator.sample.adapter;

import com.codethen.mongo.adapter.generator.sample.AddressExt;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import org.bson.Document;

@SuppressWarnings("unchecked")
public class AddressExtAdapter<T extends AddressExt> extends AddressAdapter<T> {
  public static final Fields fields = new Fields();

  public static final AddressExtAdapter<AddressExt> INSTANCE = new AddressExtAdapter<>();

  @Override
  public T newModelInstance() {
    return (T) new AddressExt();
  }

  @Override
  public Document model2doc(T model) {
    final Document doc = super.model2doc(model);
    if (doc == null) return null;
    appendTo(doc, fields.city, model.getCity());
    return doc;
  }

  @Override
  public T doc2model(Document doc) {
    final T model = super.doc2model(doc);
    if (model == null) return null;
    model.setCity((String) doc.get(fields.city));
    return model;
  }

  public static class Fields extends AddressAdapter.Fields {
    public final String city = "city";
  }
}
