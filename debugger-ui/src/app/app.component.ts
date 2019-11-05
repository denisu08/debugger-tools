import {Component, ChangeDetectorRef, OnInit, OnDestroy} from '@angular/core';
import {NgbModal, NgbModalOptions} from '@ng-bootstrap/ng-bootstrap';
import {AddVariableModalComponent} from 'src/add-variable-modal/add-variable-modal.component';
import {COMMAND_TYPE, COMMAND_PARAM} from 'src/util/app-constant';
import {HttpClient} from '@angular/common/http';
import {WebSocketAPI} from './WebSocketAPI';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Debugger App';

  webSocketAPI: WebSocketAPI;

  // connection setting
  ipValue: string;
  portValue: string;

  options: NgbModalOptions;

  readonly COMMAND_TYPE = COMMAND_TYPE;
  readonly COMMAND_PARAM = COMMAND_PARAM;

  // roomId === serviceId
  roomId = 'BOFrontEndUserManagerService';    // dummy

  constructor(private cdr: ChangeDetectorRef, public modalService: NgbModal, public httpClient: HttpClient) {
    vex.defaultOptions.className = 'vex-theme-os';
  }

  ngOnInit() {
    this.webSocketAPI = new WebSocketAPI(this.roomId, this);
    this.webSocketAPI._connect();
    this.ipValue = '127.0.0.1';
    this.portValue = '8888';
  }

  ngOnDestroy() {
    if (this.webSocketAPI) {
      this.webSocketAPI._disconnect();
      this.webSocketAPI = null;
    }
  }

  isConnected() {
    return this.webSocketAPI.isConnected;
  }

  isDebugConnected() {
    return this.webSocketAPI.isConnected && this.isFlag(COMMAND_PARAM.IS_CONNECT);
  }

  executeAction(command, pData = {}) {
    this.webSocketAPI._executeAction(command, this.roomId, pData);
  }

  isFlag(command) {
    return this.webSocketAPI.queryDataByKey(command);
  }

  isShowVariable() {
    return this.isFlag(COMMAND_PARAM.IS_CONNECT);
  }

  _forceDraw() {
    this.cdr.detectChanges();
  }

  getBreakpoints() {
    return this.webSocketAPI.queryDataByKey(this.COMMAND_PARAM.BREAKPOINTS);
  }

  getCurrentLineBreakpoint() {
    return this.webSocketAPI.queryDataByKey(this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT);
  }

  getVariables() {
    const allVariables = [];

    let systemVariables = [];
    if (this.webSocketAPI.queryDataByKey(COMMAND_PARAM.CURRENT_LINE_BREAKPOINT) > 0) {
      systemVariables = this.webSocketAPI.queryDataByKey(this.COMMAND_PARAM.GET_SYSTEM_VARIABLES);
    }
    const customVariables = this.webSocketAPI.queryDataByKey(this.COMMAND_PARAM.GET_CUSTOM_VARIABLES);
    Object.keys(customVariables).forEach(e => {
      allVariables.push({key: e, value: customVariables[e], isSystem: false});
    });
    Object.keys(systemVariables).forEach(e => {
      allVariables.push({key: e, value: systemVariables[e], isSystem: true});
    });
    return allVariables;
  }

  addVariable() {
    this.options = {
      size: 'sm',
      backdrop: 'static',
      keyboard: false
    };
    const modalRef = this.modalService.open(AddVariableModalComponent, this.options);
    modalRef.componentInstance.saveCallback = this.onChangeVariable.bind(this);
  }

  deleteVariable(variableKey) {
    this.onChangeVariable(variableKey, false);
  }

  onChangeVariable(pData, isAdd) {
    if (isAdd) {
      this.executeAction(this.COMMAND_TYPE.ADD_VARIABLE, pData);
    } else {
      this.executeAction(this.COMMAND_TYPE.REMOVE_VARIABLE, pData);
    }
  }

  // sendMessage() {
  //   this.webSocketAPI._send(this.name);
  // }

  // handleMessage(message) {
  //   this.greeting = message;
  //   console.log(`message: ${message}`);
  // }
}
