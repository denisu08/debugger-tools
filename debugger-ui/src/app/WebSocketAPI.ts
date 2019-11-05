import * as Stomp from 'stompjs';
import * as SockJS from 'sockjs-client';
import {AppComponent} from './app.component';
import {COMMAND_TYPE, COMMAND_PARAM} from 'src/util/app-constant';

export class WebSocketAPI {
  basePATH = 'http://localhost:8080';
  webSocketEndPoint = `${this.basePATH}/ws`;
  baseTopic = '/app/debugger/#{param}';
  stompClient: any;
  appComponent: AppComponent;
  data: any;
  isConnected: boolean;
  serviceId: string;

  tm: any;

  readonly COMMAND_TYPE = COMMAND_TYPE;
  readonly COMMAND_PARAM = COMMAND_PARAM;
  readonly DEFAULT_CURRENT_LINE_BREAKPOINT = 0;

  constructor(serviceId: string, appComponent: AppComponent) {
    this.data = {
      [this.COMMAND_PARAM.IP]: '',
      [this.COMMAND_PARAM.PORT]: '',
      [this.COMMAND_PARAM.IS_CONNECT]: false,
      [this.COMMAND_PARAM.IS_MUTE]: false,
      [this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT]: this.DEFAULT_CURRENT_LINE_BREAKPOINT,
      [this.COMMAND_PARAM.BREAKPOINTS]: [],
      [this.COMMAND_PARAM.GET_SYSTEM_VARIABLES]: {
        session: 'SKJKBSDHBDJHKBSHBSUIY726SSD',
        idx: '12',
        flowId: 'PCCUploadFile'
      },
      [this.COMMAND_PARAM.GET_CUSTOM_VARIABLES]: {}
    };
    this.appComponent = appComponent;
    this.isConnected = false;
    this.serviceId = serviceId;
  }

  _getTopic(pTopic: string) {
    return this.baseTopic.replace('#{param}', pTopic);
  }

  _connect() {
    console.log('Initialize WebSocket Connection');
    const ws = new SockJS(this.webSocketEndPoint);
    this.stompClient = Stomp.over(ws);
    /*this.stompClient.onopen = () => {
      setInterval(this.ping, 1000);
    };*/

    const that = this;
    that.stompClient.connect({}, (frame) => {
      that.stompClient.subscribe(`/debug-channel/${that.serviceId}`, (sdkEvent) => {
        const msg = sdkEvent.data;
        if (msg === '__pong__') {
          that.pong();
        } else {
          that.onMessageReceived(sdkEvent);
        }
      });
      that.stompClient.heartbeat.outgoing = 20000;
      that.stompClient.heartbeat.incoming = 0;
      that.stompClient.reconnect_delay = 2000;
      that.isConnected = true;

      that.appComponent.httpClient.get(`${that.basePATH}/api/service/${that.serviceId}`, {
        observe: 'response'
      })
        .toPromise()
        .then(response => {
          this.data[this.COMMAND_PARAM.BREAKPOINTS] = response.body;
        })
        .catch(console.log);

      that.appComponent._forceDraw();
    }, this.errorCallBack.bind(this));
  }

  ping() {
    this.stompClient.send('__ping__');
    this.tm = setTimeout(() => {
    }, 5000);
  }

  pong() {
    clearTimeout(this.tm);
  }

  _disconnect() {
    const that = this;
    if (this.stompClient !== null) {
      this.stompClient.disconnect(() => {
        that.isConnected = false;
        that.appComponent._forceDraw();
      });
    }
    console.log('Disconnected');
  }

  queryDataByKey(type: string) {
    return this.data[type];
  }

  _executeAction(commandType: string, roomId: string, pData: any) {
    console.log('_executeAction: ' + commandType, roomId, pData);
    switch (commandType) {
      case this.COMMAND_TYPE.CONNECT:
      case this.COMMAND_TYPE.DISCONNECT:
      case this.COMMAND_TYPE.RESUME:    // this._data[this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT] = this.DEFAULT_CURRENT_LINE_BREAKPOINT;
      case this.COMMAND_TYPE.NEXT:      // this._data[this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT] += 1;
        this._sendCommand(roomId, {type: commandType});
        break;
      case this.COMMAND_TYPE.MUTE:
        this.data[this.COMMAND_PARAM.IS_MUTE] = !this.data[this.COMMAND_PARAM.IS_MUTE];
        break;
      case this.COMMAND_TYPE.SET_BREAKPOINT:
        this.setBreakpoint(pData.line, pData.flag);
        break;
      case this.COMMAND_TYPE.ADD_VARIABLE:
        this.addVariable(pData);
        break;
      case this.COMMAND_TYPE.REMOVE_VARIABLE:
        this.removeVariable(pData);
        break;
      default:
        break;
    }
  }

  _sendCommand(roomId: string, body: any) {
    this.stompClient.send(
      this._getTopic(`${roomId}`),
      {},
      JSON.stringify(body)
    );
  }

  setBreakpoint(pLine, pFlag) {
    this.data[this.COMMAND_PARAM.BREAKPOINTS].forEach(e => {
      if (e.line === pLine) {
        e.isDebug = pFlag;
      }
    });
  }

  addVariable(pData) {
    this.data[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES] = Object.assign(this.data[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES], {[pData]: ''});
  }

  removeVariable(pKey) {
    delete this.data[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES][`${pKey}`];
  }

  // on error, schedule a reconnection attempt
  errorCallBack(error) {
    console.log('errorCallBack -> ' + error);
    setTimeout(() => {
      this._connect();
    }, 5000);
  }

  /**
   * Send message to sever via web socket
   * @param {*} message
   */
  _send(message) {
    console.log('calling logout api via web socket');
    this.stompClient.send('/app/hello', {}, JSON.stringify(message));
  }

  sendMessage(message) {
    this.stompClient.send('/app/send/message', {}, message);
  }

  onMessageReceived(message) {
    console.log('Message Receieved from Server :: ' + message);
    const bodyList = message.body.split('#');
    bodyList.forEach(e => {
      const bodyMsg = e.split('::');
      this.data[bodyMsg[0]] = bodyMsg[1];
    });
  }
}
