package org.vrspace.server.obj;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.vrspace.server.core.Scene;
import org.vrspace.server.dto.SceneProperties;
import org.vrspace.server.dto.VREvent;
import org.vrspace.server.types.Owned;
import org.vrspace.server.types.Private;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = false)
@NodeEntity
@Owned
@Slf4j
public class Client extends VRObject {
  @Index(unique = true)
  private String name;
  @Transient
  transient private Point leftArmPos;
  @Transient
  transient private Point rightArmPos;
  @Transient
  transient private Quaternion leftArmRot;
  @Transient
  transient private Quaternion rightArmRot;
  @Transient
  transient private Double userHeight;

  // CHECKME: this needs to get refactored eventually
  @JsonIgnore
  private Set<VRObject> owned;

  @Private
  @Transient
  transient private SceneProperties sceneProperties;

  // CHECKME OpenVidu token; should that be Map tokens?
  @Private
  @Transient
  transient private String token;

  @JsonIgnore
  @Transient
  transient private ConcurrentWebSocketSessionDecorator session;
  @JsonIgnore
  @Transient
  transient private Scene scene;
  @JsonIgnore
  @Transient
  transient private ObjectMapper mapper;
  @JsonIgnore
  @Transient
  transient private boolean guest;

  public Client() {
    super();
    this.setActive(true);
  }

  // used in tests
  public Client(Long id) {
    this();
    this.setId(id);
  }

  // used in tests
  public Client(String name) {
    this();
    this.name = name;
  }

  public Client(ConcurrentWebSocketSessionDecorator session) {
    this();
    this.session = session;
  }

  @Override
  public void processEvent(VREvent event) {
    if (!event.getSource().isActive()) {
      // stop listening to inactive objects (disconnected clients)
      event.getSource().removeListener(this);
    } else if (event.getPayload() == null) {
      // serialize event in the context of client
      sendMessage(event);
    } else {
      // event is already serialized by dispatcher
      sendMessage(event.getPayload());
    }
  }

  private void sendMessage(String json) {
    log.debug(getObjectId() + " Received " + json);
    if (session.isOpen()) {
      try {
        session.sendMessage(new TextMessage(json));
      } catch (IOException e) {
        log.warn("Can't send message " + json + ": " + e);
      } catch (IllegalStateException e) {
        log.warn("Can't send message " + json + ": " + e);
      }
    } else {
      log.debug("Session closed, message ignored: " + json);
    }
  }

  public void sendMessage(Object obj) {
    try {
      sendMessage(mapper.writeValueAsString(obj));
    } catch (Exception e) {
      // I don't see how this can happen, but if it does, make sure it's logged
      log.error("Can't send message " + obj, e);
    }

  }

  public void addOwned(VRObject... objects) {
    if (owned == null) {
      owned = new HashSet<VRObject>();
    }
    for (VRObject obj : objects) {
      owned.add(obj);
    }
  }

  public void removeOwned(VRObject... objects) {
    if (owned != null) {
      for (VRObject obj : objects) {
        owned.remove(obj);
      }
    }
  }

  public boolean isOwner(VRObject obj) {
    return this.equals(obj) || owned != null && obj != null && owned.contains(obj);
  }

}
