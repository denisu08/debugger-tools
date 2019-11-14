import * as Stomp from 'stompjs';
import * as SockJS from 'sockjs-client';
import {AppComponent} from './app.component';
import {COMMAND_TYPE, COMMAND_PARAM} from 'src/util/app-constant';

export class DebuggerClientApi {
  basePATH = 'http://localhost:8080';
  webSocketEndPoint = `${this.basePATH}/ws`;
  baseTopic = '/app/debugger/#{param}';
  stompClient: any;
  appComponent: AppComponent;
  data: any;
  isConnected: boolean;
  waitResponse: boolean;

  breakpointReadyFlag: boolean;

  tm: any;

  readonly COMMAND_TYPE = COMMAND_TYPE;
  readonly COMMAND_PARAM = COMMAND_PARAM;
  readonly DEFAULT_CURRENT_LINE_BREAKPOINT = '0';
  readonly DEFAULT_POINTER_BREAKPOINT = 'xx';

  constructor(appComponent: AppComponent) {
    this.data = {
      [this.COMMAND_PARAM.IP]: '',
      [this.COMMAND_PARAM.PORT]: '',
      [this.COMMAND_PARAM.IS_CONNECT]: false,
      [this.COMMAND_PARAM.IS_MUTE]: false,
      // add segment to
      [this.COMMAND_PARAM.CURRENT_POINTER_BREAKPOINT]: this.DEFAULT_POINTER_BREAKPOINT,
      [this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT]: this.DEFAULT_CURRENT_LINE_BREAKPOINT,
      [this.COMMAND_PARAM.BREAKPOINTS]: {},
      [this.COMMAND_PARAM.GET_SYSTEM_VARIABLES]: {},
      [this.COMMAND_PARAM.GET_CUSTOM_VARIABLES]: {}
    };
    this.appComponent = appComponent;
    this.isConnected = false;
    this.waitResponse = false;
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
    that.stompClient.connect(
      {
        processFlowGeneratorId: this.appComponent.processFlowGeneratorId,
        sourcePathJar: './testBundle/ProcessFlow_aldis_menuFlow-service-1.0.jar'
      },
      (frame) => {
        that.stompClient.subscribe(`/debug-channel/${that.appComponent.processFlowGeneratorId}`, (sdkEvent) => {
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

        this.patchBreakpointFromService(that);

        that.appComponent._forceDraw();
      }, this.errorCallBack.bind(this));
  }

  patchBreakpointFromService(that: DebuggerClientApi) {
    const isExist = that.appComponent.getFunctionId() in that.data[that.COMMAND_PARAM.BREAKPOINTS];
    if (!isExist) {
      that.appComponent.httpClient.get(`${that.basePATH}/api/processFlowGenerator/${that.appComponent.processFlowGeneratorId}`, {
        observe: 'response'
      })
        .toPromise()
        .then(response => {

          this.appComponent.listProcessFlowGenerator = response.body;
          const processFlowGeneratorKeys = Object.keys(response.body);
          processFlowGeneratorKeys.forEach(processFlowId => {
            if (!this.appComponent.processFlowId) {
              this.appComponent.processFlowId = processFlowId;
            }
            Object.keys(response.body[processFlowId]).forEach(functionId => {
              if (this.appComponent.getFunctionId() === ' - ') {
                this.appComponent.setFunctionId(functionId);
              }
              this.data[this.COMMAND_PARAM.BREAKPOINTS][functionId] = response.body[processFlowId][functionId];
            });
          });

          const payload = {[this.COMMAND_PARAM.BREAKPOINTS]: this.data[this.COMMAND_PARAM.BREAKPOINTS]};
          this._sendCommand(this.appComponent.processFlowGeneratorId,
            {functionId: this.appComponent.getFunctionId(), type: this.COMMAND_TYPE.SYNC, content: btoa(JSON.stringify(payload))});

          this.breakpointReadyFlag = true;
        })
        .catch(console.log);
    }

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

  _executeAction(commandType: string, pData: any) {
    console.log('_executeAction: ' + commandType, pData);
    const payload = {};
    switch (commandType) {
      case this.COMMAND_TYPE.DISCONNECT:
        this.waitResponse = true;
        break;
      case this.COMMAND_TYPE.RESUME:    // this._data[this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT] = this.DEFAULT_CURRENT_LINE_BREAKPOINT;
      case this.COMMAND_TYPE.NEXT:      // this._data[this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT] += 1;
        break;
      case this.COMMAND_TYPE.MUTE:
        payload[this.COMMAND_PARAM.IS_MUTE] = !this.data[this.COMMAND_PARAM.IS_MUTE];
        break;
      case this.COMMAND_TYPE.CONNECT:
        payload[this.COMMAND_PARAM.IP] = pData.ip;
        payload[this.COMMAND_PARAM.PORT] = pData.port;
        payload[this.COMMAND_PARAM.BREAKPOINTS] = this.data[this.COMMAND_PARAM.BREAKPOINTS];
        this.waitResponse = true;
        break;
      case this.COMMAND_TYPE.SET_BREAKPOINT:
        payload[this.COMMAND_PARAM.CURRENT_BREAKPOINTS] = this.changeBreakpoint(pData.line, pData.flag);
        break;
      case this.COMMAND_TYPE.ADD_VARIABLE:
        this.addVariable(pData);
        commandType = this.COMMAND_TYPE.SET_VARIABLE;
        payload[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES] = this.data[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES];
        break;
      case this.COMMAND_TYPE.REMOVE_VARIABLE:
        this.removeVariable(pData);
        commandType = this.COMMAND_TYPE.SET_VARIABLE;
        payload[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES] = this.data[this.COMMAND_PARAM.GET_CUSTOM_VARIABLES];
        break;
      default:
        break;
    }
    // send command to backend
    this.data[this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT] = this.DEFAULT_CURRENT_LINE_BREAKPOINT;
    this._sendCommand(this.appComponent.processFlowGeneratorId,
      {functionId: this.appComponent.getFunctionId(), type: commandType, content: btoa(JSON.stringify(payload))});
  }

  _waitResponse() {

  }

  _sendCommand(processFlowGeneratorId: string, body: any) {
    this.stompClient.send(
      this._getTopic(`${processFlowGeneratorId}`),
      {},
      JSON.stringify(body)
    );
  }

  changeBreakpoint(pLine, pFlag) {
    const newBreakpoints = [].concat(this.data[this.COMMAND_PARAM.BREAKPOINTS][this.appComponent.getFunctionId()]);
    newBreakpoints.forEach(e => {
      if (e.line === pLine) {
        e.isDebug = pFlag;
      }
    });
    return newBreakpoints;
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

  onMessageReceived(message) {
    console.log('Message Receieved from Server :: ' + message);
    this.waitResponse = false;
    if (message.body.indexOf('error#') === 0) {
      vex.dialog.alert(message.body.replace('error#', ''));
    } else {
      this.data = JSON.parse(message.body);
      if (this.COMMAND_PARAM.CURRENT_POINTER_BREAKPOINT in this.data
        && this.data[this.COMMAND_PARAM.CURRENT_POINTER_BREAKPOINT]
        && this.data[this.COMMAND_PARAM.CURRENT_POINTER_BREAKPOINT] !== this.DEFAULT_POINTER_BREAKPOINT) {
        const params = this.data[this.COMMAND_PARAM.CURRENT_POINTER_BREAKPOINT].split('#');
        this.appComponent.setFunctionId(params[0]);
        this.data[this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT] = params[1];
      }
    }
  }
}
