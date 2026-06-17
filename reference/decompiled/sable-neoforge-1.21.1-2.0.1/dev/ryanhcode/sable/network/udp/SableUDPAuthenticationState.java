package dev.ryanhcode.sable.network.udp;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SableUDPAuthenticationState {
   private SableUDPAuthenticationState.State state;
   @Nullable
   private UUID outgoingToken;
   private long tokenAssignmentTime;
   @Nullable
   private InetSocketAddress activeAddress;
   private int lastAlivePingIndex;

   public SableUDPAuthenticationState(UUID token) {
      this.assignToken(token);
   }

   public SableUDPAuthenticationState.State getState() {
      return this.state;
   }

   @Nullable
   public InetSocketAddress getActiveAddress() {
      return this.activeAddress;
   }

   public boolean isExpectedToken(UUID token) {
      return this.outgoingToken != null && this.outgoingToken.equals(token);
   }

   public void assignToken(UUID token) {
      this.outgoingToken = token;
      this.tokenAssignmentTime = System.currentTimeMillis();
      this.state = SableUDPAuthenticationState.State.AWAITING_AUTH;
   }

   public void assignAddress(@NotNull InetSocketAddress address) {
      this.activeAddress = address;
      this.state = SableUDPAuthenticationState.State.AUTHENTICATED;
      this.tokenAssignmentTime = -1L;
      this.outgoingToken = null;
   }

   public void setLastAlivePingIndex(int pingIndex) {
      this.lastAlivePingIndex = pingIndex;
   }

   public int getLastAlivePingIndex() {
      return this.lastAlivePingIndex;
   }

   public static enum State {
      AWAITING_AUTH,
      AWAITING_CHALLENGE,
      AUTHENTICATED;
   }
}
