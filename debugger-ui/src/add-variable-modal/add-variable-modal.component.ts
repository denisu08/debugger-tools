import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-variable-modal',
  templateUrl: './add-variable-modal.component.html',
  styleUrls: ['./add-variable-modal.component.css']
})
export class AddVariableModalComponent implements OnInit {

  variableKey: string = '';
  saveCallback: Function;

  constructor(public activeModal: NgbActiveModal) {
  }

  ngOnInit() {
  }

  save() {
    if (!this.variableKey) {
      vex.dialog.alert('Variable key is mandatory');
    } else {
      this.saveCallback(this.variableKey, true);
      this.activeModal.close();
    }
  }

  closeModal() {
    this.activeModal.close('Modal Closed');
  }
}
