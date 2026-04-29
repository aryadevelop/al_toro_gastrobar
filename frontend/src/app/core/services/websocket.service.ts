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
    const apiBase = environment.apiBaseUrl;
    // apiBaseUrl = 'http://localhost:8080/api' -> ws://localhost:8080/ws
    const httpBase = apiBase.replace(/\/api$/, '');
    return httpBase.replace(/^http/, 'ws') + '/ws';
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
}
