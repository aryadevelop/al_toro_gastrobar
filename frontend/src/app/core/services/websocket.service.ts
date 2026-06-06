import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Servicio genérico de WebSocket STOMP para suscribirse a tópicos del backend.
 *
 * Usa @stomp/stompjs con WebSocket nativo (sin SockJS) conectándose a ws://host/ws.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private readonly subscriptions = new Map<string, Subject<unknown>>();

  private getWsUrl(): string {
    const apiBase = environment.apiBaseUrl;        // dev absoluto | prod relativo ('/api')
    const httpBase = apiBase.replace(/\/api$/, ''); // quita el sufijo '/api'

    // apiBaseUrl absoluto (dev): convertir el esquema http(s) -> ws(s).
    if (/^https?:\/\//.test(httpBase)) {
      return httpBase.replace(/^http/, 'ws') + '/ws';
    }

    // apiBaseUrl relativo (prod): construir desde el origen actual.
    // https -> wss (obligatorio sobre TLS); http -> ws.
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${scheme}://${window.location.host}${httpBase}/ws`;
  }

  private ensureConnected(): Client {
    if (this.client && this.client.connected) {
      return this.client;
    }

    if (this.client) {
      return this.client;
    }

    this.client = new Client({
      brokerURL: this.getWsUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.onConnect = () => {
      // Re-subscribe all pending subscriptions on reconnect
      this.subscriptions.forEach((_, topic) => {
        this.doSubscribe(topic);
      });
    };

    this.client.activate();
    return this.client;
  }

  /**
   * Subscribe to a STOMP topic. Returns an Observable that emits parsed JSON messages.
   * Automatically connects/reconnects as needed.
   */
  subscribe<T>(topic: string): Observable<T> {
    if (this.subscriptions.has(topic)) {
      return this.subscriptions.get(topic)!.asObservable() as Observable<T>;
    }

    const subject = new Subject<unknown>();
    this.subscriptions.set(topic, subject);

    const client = this.ensureConnected();
    if (client.connected) {
      this.doSubscribe(topic);
    }

    return subject.asObservable() as Observable<T>;
  }

  private doSubscribe(topic: string): void {
    const subject = this.subscriptions.get(topic);
    if (!subject || !this.client) {
      return;
    }

    this.client.subscribe(topic, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body);
        subject.next(parsed);
      } catch {
        subject.next(message.body);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((subject) => subject.complete());
    this.subscriptions.clear();

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  /**
   * Envía un mensaje a un destino STOMP.
   */
  sendMessage(destination: string, body: any): void {
    const client = this.ensureConnected();
    if (client.connected) {
      client.publish({ destination, body: JSON.stringify(body) });
    } else {
      // If not connected, wait for connect to send (optional advanced logic).
      // For now, we attempt to publish if it's connected, or we could queue it.
      // But standard STOMP over websockets usually requires connection first.
      console.warn('STOMP client not connected. Message not sent.');
    }
  }
}
