package org.vrspace.server.core;

import org.springframework.http.HttpHeaders;
import org.vrspace.server.obj.Client;

/**
 * Client factory interface, providing methods required to log in into the
 * server. Factory methods are passed client name if available, i.e. if client
 * HTTP session is authorised. All session HTTP headers are passed every method.
 * 
 * @author joe
 * @see WorldManager#login(org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator)
 *
 */
public interface ClientFactory {
  /**
   * Find an authorised known client, called only if security principal is known.
   * Default implementation calls db.getClientByName(name).
   * 
   * @param name    client (security principal) name
   * @param db      database repository
   * @param headers all HTTP headers
   * @return a client found in the database or elsewhere
   */
  public default Client findClient(String name, VRObjectRepository db, HttpHeaders headers) {
    return db.getClientByName(name);
  }

  /**
   * Create a new guest client, called only if server configuration allows for
   * anonymous guest clients, and client name (security principal) is unknown.
   * Default implementation does not create a client.
   * 
   * @param headers all HTTP headers
   * @return new Client instance, null by default
   */
  public default Client createGuestClient(HttpHeaders headers) {
    return null;
  }

  /**
   * Called if guest clients are not allowed, and user name (security principal)
   * is unknown. Implementation may yet return a client based on headers
   * available. Default implementation returns null.
   * 
   * @param headers all HTTP headers
   * @return a Client determined by headers, null by default
   */
  public default Client handleUnknownClient(HttpHeaders headers) {
    return null;
  }
}
