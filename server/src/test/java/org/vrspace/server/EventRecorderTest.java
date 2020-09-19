package org.vrspace.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.vrspace.server.core.Scene;
import org.vrspace.server.core.WorldManager;
import org.vrspace.server.dto.SceneProperties;
import org.vrspace.server.dto.VREvent;
import org.vrspace.server.obj.Client;
import org.vrspace.server.obj.EventRecorder;
import org.vrspace.server.obj.Point;
import org.vrspace.server.obj.VRObject;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
public class EventRecorderTest {
  @Mock
  WorldManager worldManager;
  @Mock
  WebSocketSession recordingSession;
  @Mock
  WebSocketSession playingSession;
  @Captor
  private ArgumentCaptor<WebSocketMessage<?>> message;

  ObjectMapper mapper = new ObjectMapper();

  Set<VRObject> transforms = new HashSet<VRObject>();
  Set<VRObject> permanents = new HashSet<VRObject>();
  VRObject active = new VRObject(1L, 0, 0, 0, new VRObject(11L).active()).active();
  Client client = new Client();

  @Before
  public void setup() throws Exception {
    transforms.add(active);
    transforms.add(new VRObject(2L, 1, 0, 0).passive());

    permanents.add(new VRObject(101L, new VRObject(12L)));
    permanents.add(new VRObject(202L));

    when(worldManager.getRange(any(Client.class), any(Point.class), any(Point.class))).thenReturn(transforms);
    when(worldManager.getPermanents(any(Client.class))).thenReturn(permanents);

    doNothing().when(playingSession).sendMessage(message.capture());

    // recording client
    client.setPosition(new Point());
    client.setMapper(mapper);
    client.setSceneProperties(new SceneProperties());
    client.setSession(recordingSession);
    Scene scene = new Scene(worldManager, client);
    client.setScene(scene);
  }

  private void record(EventRecorder recorder) {
    // recording own event:
    VREvent ownEvent = new VREvent(client, client);
    ownEvent.addChange("mesh", "dolphin.glb");
    client.notifyListeners(ownEvent);
    assertEquals(1, recorder.getEvents().size());

    // scene update should be the same:
    client.getScene().update();
    recorder.getScene().update();
    assertEquals(2, recorder.getEvents().size());

    // event from another client/active object:
    VREvent otherEvent = new VREvent(active, client);
    otherEvent.addChange("name", "renamed");
    active.notifyListeners(otherEvent);
    assertEquals(3, recorder.getEvents().size());
  }

  @Test
  public void testRecordAndPlay() throws Exception {
    // start recording
    EventRecorder recorder = new EventRecorder(worldManager, client, "test");
    recorder.setLoop(false);
    recorder.start();

    // record, assert changes
    record(recorder);

    // stop recording
    recorder.stop();
    // make sure recording has stopped
    client.notifyListeners(new VREvent(client, client));
    assertEquals(3, recorder.getEvents().size());

    // playing client
    Client viewer = new Client();
    viewer.setMapper(mapper);
    viewer.setSession(playingSession);

    // test playing
    recorder.play(viewer);
    Thread.sleep(500);

    // all recorded events sent to the viewer
    verify(playingSession, times(3)).sendMessage(any(TextMessage.class));
  }

  @Test
  public void testRecordAndLoop() throws Exception {
    // start recording
    EventRecorder recorder = new EventRecorder(worldManager, client, "test");
    recorder.setLoop(true);
    recorder.start();

    // record, assert changes
    record(recorder);

    // stop recording
    recorder.stop();
    assertEquals(4, recorder.getEvents().size());
    // make sure recording has stopped
    client.notifyListeners(new VREvent(client, client));
    assertEquals(4, recorder.getEvents().size());

    // playing client
    Client viewer = new Client();
    viewer.setMapper(mapper);
    viewer.setSession(playingSession);

    // test playing
    recorder.play(viewer);
    Thread.sleep(500);
    // CHECKME: valid way to test that loop has stopped?
    recorder.setLoop(false);
    Thread.sleep(100);
    verify(playingSession, atLeast(4)).sendMessage(any(TextMessage.class));
    Thread.sleep(100);
    verifyNoMoreInteractions(playingSession);

  }

}
