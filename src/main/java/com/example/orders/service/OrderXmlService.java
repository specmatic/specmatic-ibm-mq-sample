package com.example.orders.service;

import com.example.orders.model.Order;
import com.example.orders.model.OrderItem;
import com.example.orders.model.OrderRequest;
import com.example.orders.model.OrderStatus;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Service
public class OrderXmlService {
  private static final String NAMESPACE = "urn:specmatic:orders";

  public OrderRequest readOrderRequest(String xml) {
    Element root = parse(xml).getDocumentElement();
    requireElement(root, "OrderRequest");
    Element items = child(root, "orderItems");
    List<OrderItem> orderItems = new ArrayList<>();
    for (Element item : children(items, "item")) {
      orderItems.add(new OrderItem(
          integer(child(item, "id")), text(child(item, "name")),
          integer(child(item, "quantity")), Double.valueOf(text(child(item, "price")))));
    }
    return new OrderRequest(integer(child(root, "id")), orderItems);
  }

  public String writeOrder(Order order) {
    return document("Order", element -> {
      append(element, "id", order.id());
      append(element, "totalAmount", order.totalAmount());
      append(element, "status", order.status());
    });
  }

  private static org.w3c.dom.Document parse(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid order XML", ex);
    }
  }

  private static String document(String rootName, java.util.function.Consumer<Element> content) {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      var document = factory.newDocumentBuilder().newDocument();
      Element root = document.createElementNS(NAMESPACE, rootName);
      root.setAttribute("xmlns", NAMESPACE);
      document.appendChild(root);
      content.accept(root);
      var transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      var transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      var output = new java.io.StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(output));
      return output.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to write order XML", ex);
    }
  }

  private static Element child(Element parent, String name) {
    for (Element element : children(parent, name)) return element;
    throw new IllegalArgumentException("Missing XML element " + name);
  }

  private static List<Element> children(Element parent, String name) {
    var found = new ArrayList<Element>();
    for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && name.equals(element.getLocalName())) found.add(element);
    }
    return found;
  }

  private static void requireElement(Element element, String name) {
    if (!name.equals(element.getLocalName()) || !NAMESPACE.equals(element.getNamespaceURI()))
      throw new IllegalArgumentException("Expected {" + NAMESPACE + "}" + name);
  }

  private static String text(Element element) { return element.getTextContent().trim(); }
  private static Integer integer(Element element) { return Integer.valueOf(text(element)); }
  private static void append(Element parent, String name, Object value) {
    Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, name);
    element.setTextContent(value instanceof OrderStatus status ? status.name() : value.toString());
    parent.appendChild(element);
  }
}
