import {Component, ChangeDetectorRef, OnInit, OnDestroy} from '@angular/core';
import {NgbModal, NgbModalOptions} from '@ng-bootstrap/ng-bootstrap';
import {AddVariableModalComponent} from 'src/add-variable-modal/add-variable-modal.component';
import {COMMAND_TYPE, COMMAND_PARAM} from 'src/util/app-constant';
import {HttpClient} from '@angular/common/http';
import {DebuggerClientApi} from './DebuggerClientApi';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  webSocketAPI: DebuggerClientApi;

  // connection setting
  ipValue: string;
  portValue: string;

  options: NgbModalOptions;

  readonly COMMAND_TYPE = COMMAND_TYPE;
  readonly COMMAND_PARAM = COMMAND_PARAM;

  // processFlowGeneratorId = 'e700abce-7d8f-405d-be6b-7ec2bd5df971';
  processFlowGeneratorId = '217d7bed-fa06-43b2-85cd-eefed37e540e';
  processFlowId = '';               // as subcribe channel
  subFunctionService = '';          // [serviceId - functionId] as breakpoints debugger
  subFunctionId = '';

  listProcessFlowGenerator = {};

  constructor(private cdr: ChangeDetectorRef, public modalService: NgbModal, public httpClient: HttpClient) {
    vex.defaultOptions.className = 'vex-theme-os';
  }

  ngOnInit() {
    this.webSocketAPI = new DebuggerClientApi(this);
    this.webSocketAPI._connect();
    this.ipValue = '10.10.230.203';
    this.portValue = '8822';
  }

  ngOnDestroy() {
    if (this.webSocketAPI) {
      this.webSocketAPI._disconnect();
      this.webSocketAPI = null;
    }
  }

  isWaitResponse() {
    return this.webSocketAPI.waitResponse;
  }

  isConnected() {
    return this.webSocketAPI.isConnected;
  }

  isDebugConnected() {
    return this.webSocketAPI.isConnected && this.isFlag(COMMAND_PARAM.IS_CONNECT);
  }

  isDebugActive() {
    return this.isDebugConnected() && this.webSocketAPI.queryDataByKey(COMMAND_PARAM.CURRENT_LINE_BREAKPOINT) > 0;
  }

  executeAction(command, pData = {}) {
    this.webSocketAPI._executeAction(command, pData);
  }

  isFlag(command) {
    return this.webSocketAPI.queryDataByKey(command);
  }

  isDebugReady() {
    return this.webSocketAPI.isConnected && this.webSocketAPI.breakpointReadyFlag;
  }

  isShowVariable() {
    // return this.isFlag(COMMAND_PARAM.IS_CONNECT);
    return this.isConnected();
  }

  _forceDraw() {
    this.cdr.detectChanges();
  }

  getListProcessFlow() {
    if (this.listProcessFlowGenerator) {
      return Object.keys(this.listProcessFlowGenerator);
    }
    return [];
  }

  getListFunction() {
    if (this.listProcessFlowGenerator && this.processFlowId in this.listProcessFlowGenerator) {
      return Object.keys(this.listProcessFlowGenerator[this.processFlowId]);
    }
    return [];
  }

  getBreakpoints() {
    return this.webSocketAPI.queryDataByKey(this.COMMAND_PARAM.BREAKPOINTS)[this.getFunctionId()];
  }

  setFunctionId(value) {
    const funcSplit = value.split(' - ');
    this.subFunctionService = funcSplit[0];
    this.subFunctionId = funcSplit[1];
    this.webSocketAPI.patchBreakpointFromService(this.webSocketAPI);
  }

  getFunctionId() {
    return `${this.subFunctionService} - ${this.subFunctionId}`;
  }

  setProcessFlowId(value) {
    this.processFlowId = value;
    this.setFunctionId(Object.keys(this.listProcessFlowGenerator[this.processFlowId])[0]);
  }

  getCurrentLineBreakpoint() {
    return parseInt(this.webSocketAPI.queryDataByKey(this.COMMAND_PARAM.CURRENT_LINE_BREAKPOINT), 0);
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
}
