package er.ajax.json;

import java.util.NoSuchElementException;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCResult;
import org.jabsorb.serializer.Serializer;
import org.json.JSONException;
import org.json.JSONObject;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.WOSession;

import er.extensions.foundation.ERXMutableURL;

public class JSONRequestHandler extends WORequestHandler {
	public static final String RequestHandlerKey = "json";

	private JSONRPCBridge _jsonBridge;

	public static JSONRequestHandler register() {
		JSONRequestHandler requestHandler = new JSONRequestHandler();
		WOApplication.application().registerRequestHandler(requestHandler, JSONRequestHandler.RequestHandlerKey);
		return requestHandler;
	}

	public JSONRequestHandler() {
		_jsonBridge = JSONBridge.createBridge();
	}

	public JSONRPCBridge getJSONBridge() {
		return _jsonBridge;
	}

	public void registerSerializer(Serializer serializer) throws Exception {
		JSONRPCBridge.getSerializer().registerSerializer(serializer);
	}

	public static void registerClass(Class clazz) throws Exception {
		JSONRequestHandler.registerClass(clazz.getSimpleName(), clazz);
	}

	public static void registerClass(String name, Class clazz) throws Exception {
		JSONRPCBridge.getGlobalBridge().registerClass(name, clazz);
	}

	public void registerService(String name, Object serviceObject) {
		_jsonBridge.registerObject(name, serviceObject);
	}

	@Override
	public WOResponse handleRequest(WORequest request) {
		WOApplication application = WOApplication.application();
		application.awake();
		try {
			WOContext context = application.createContextForRequest(request);
			WOResponse response = application.createResponseInContext(context);

			Object output;
			try {
				String inputString = request.contentString();
				JSONObject input = new JSONObject(inputString);
				String wosid = request.cookieValueForKey("wosid");
				if (wosid == null) {
					ERXMutableURL url = new ERXMutableURL();
					url.setQueryParameters(request.queryString());
					wosid = url.queryParameter("wosid");
					if (wosid == null && input.has("wosid")) {
						wosid = input.getString("wosid");
					}
				}
				context._setRequestSessionID(wosid);
				WOSession session = null;
				if (context._requestSessionID() != null) {
					session = WOApplication.application().restoreSessionWithID(wosid, context);
				}
				if (session != null) {
					session.awake();
				}
				try {
					output = _jsonBridge.call(new Object[] { request, response, context }, input);
					if (context._session() != null) {
						WOSession contextSession = context._session();
						// If this is a new session, then we have to force it to be a cookie session
						if (wosid == null) {
							boolean storesIDsInCookies = contextSession.storesIDsInCookies();
							try {
								contextSession.setStoresIDsInCookies(true);
								contextSession._appendCookieToResponse(response);
							}
							finally {
								contextSession.setStoresIDsInCookies(storesIDsInCookies);
							}
						}
						else {
							contextSession._appendCookieToResponse(response);
						}
					}
					if (output != null) {
						response.appendContentString(output.toString());
					}

					if (response != null) {
						response._finalizeInContext(context);
						response.disableClientCaching();
					}
				}
				finally {
					try {
						if (session != null) {
							session.sleep();
						}
					}
					finally {
						if (context._session() != null) {
							WOApplication.application().saveSessionForContext(context);
						}
					}
				}
			}
			catch (NoSuchElementException e) {
				e.printStackTrace();
				output = new JSONRPCResult(JSONRPCResult.CODE_ERR_NOMETHOD, null, JSONRPCResult.MSG_ERR_NOMETHOD);
			}
			catch (JSONException e) {
				e.printStackTrace();
				output = new JSONRPCResult(JSONRPCResult.CODE_ERR_PARSE, null, JSONRPCResult.MSG_ERR_PARSE);
			}
			catch (Throwable t) {
				t.printStackTrace();
				output = new JSONRPCResult(JSONRPCResult.CODE_ERR_PARSE, null, t.getMessage());
			}

			return response;
		}
		finally {
			application.sleep();
		}
	}
}