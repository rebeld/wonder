package er.ajax;

import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOComponent;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

public class AjaxOption {
  public static AjaxOption.Type DEFAULT = new AjaxOption.Type(0);
  public static AjaxOption.Type STRING = new AjaxOption.Type(1);
  public static AjaxOption.Type SCRIPT = new AjaxOption.Type(2);
  public static AjaxOption.Type NUMBER = new AjaxOption.Type(3);
  public static AjaxOption.Type ARRAY = new AjaxOption.Type(4);
  public static AjaxOption.Type STRING_ARRAY = new AjaxOption.Type(5);
  public static AjaxOption.Type BOOLEAN = new AjaxOption.Type(6);
  public static AjaxOption.Type STRING_OR_ARRAY = new AjaxOption.Type(7);

  public static class Type {
    private int _number;

    public Type(int number) {
      _number = number;
    }
  }

  private String _name;
  private AjaxOption.Type _type;

  public AjaxOption(String name) {
    this(name, AjaxOption.DEFAULT);
  }

  public AjaxOption(String name, AjaxOption.Type type) {
    _name = name;
    _type = type;
  }

  public String name() {
    return _name;
  }

  public AjaxOption.Type type() {
    return _type;
  }

  public String processValue(Object value) {
    String strValue;
    
    AjaxOption.Type type = _type;
    
    if (type == AjaxOption.STRING_OR_ARRAY) {
      if (value == null) {
      }
      else if (value instanceof NSArray) {
        type = AjaxOption.ARRAY;
      }
      else if (value instanceof String) {
        strValue = (String)value;
        if (strValue.startsWith("[")) {
          type = AjaxOption.ARRAY;
        }
        else {
          type = AjaxOption.STRING;
        }
      }
    }

    if (value == null) {
      strValue = null;
    }
    else if (type == AjaxOption.STRING) {
      strValue = "'" + value + "'";
    }
    else if (type == AjaxOption.ARRAY) {
      if (value instanceof NSArray) {
        NSArray arrayValue = (NSArray) value;
        if (arrayValue.count() == 1) {
          strValue = arrayValue.objectAtIndex(0).toString();
        }
        else {
          strValue = "[" + arrayValue.componentsJoinedByString(",") + "]";
        }
      }
      else {
        strValue = value.toString();
      }
    }
    else if (type == AjaxOption.STRING_ARRAY) {
      if (value instanceof NSArray) {
        NSArray arrayValue = (NSArray) value;
        int count = arrayValue.count();
        if (count == 1) {
          strValue = "'" + arrayValue.objectAtIndex(0).toString() + "'";
        }
        else if (count > 0) {
          strValue = "['" + arrayValue.componentsJoinedByString("','") + "']";
        }
        else {
          strValue = "[]";
        }
      }
      else {
        strValue = value.toString();
      }
    }
    else {
      strValue = value.toString();
    }
    return strValue;
  }

  public void addToDictionary(WOComponent component, NSMutableDictionary dictionary) {
    addToDictionary(_name, component, dictionary);
  }

  public void addToDictionary(String bindingName, WOComponent component, NSMutableDictionary dictionary) {
    Object value = component.valueForBinding(bindingName);
    if (value instanceof WOAssociation) {
      WOAssociation association = (WOAssociation) value;
      value = association.valueInComponent(component);
    }
    String strValue = processValue(value);
    if (strValue != null) {
      dictionary.setObjectForKey(strValue, _name);
    }
  }

  public void addToDictionary(WOComponent component, NSDictionary associations, NSMutableDictionary dictionary) {
    addToDictionary(_name, component, associations, dictionary);
  }

  public void addToDictionary(String bindingName, WOComponent component, NSDictionary associations, NSMutableDictionary dictionary) {
    if (associations != null) {
      Object value = associations.objectForKey(bindingName);
      if (value instanceof WOAssociation) {
        WOAssociation association = (WOAssociation) value;
        value = association.valueInComponent(component);
      }
      String strValue = processValue(value);
      if (strValue != null) {
        dictionary.setObjectForKey(strValue, _name);
      }
    }
  }

  public static NSMutableDictionary createAjaxOptionsDictionary(NSArray ajaxOptions, WOComponent component) {
    NSMutableDictionary optionsDictionary = new NSMutableDictionary();
    int ajaxOptionCount = ajaxOptions.count();
    for (int i = 0; i < ajaxOptionCount; i++) {
      AjaxOption ajaxOption = (AjaxOption) ajaxOptions.objectAtIndex(i);
      ajaxOption.addToDictionary(component, optionsDictionary);
    }
    return optionsDictionary;
  }

  public static NSMutableDictionary createAjaxOptionsDictionary(NSArray ajaxOptions, WOComponent component, NSDictionary associations) {
    NSMutableDictionary optionsDictionary = new NSMutableDictionary();
    int ajaxOptionCount = ajaxOptions.count();
    for (int i = 0; i < ajaxOptionCount; i++) {
      AjaxOption ajaxOption = (AjaxOption) ajaxOptions.objectAtIndex(i);
      ajaxOption.addToDictionary(component, associations, optionsDictionary);
    }
    return optionsDictionary;
  }
}