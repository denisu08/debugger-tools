import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {IConfig, NgxMaskModule} from 'ngx-mask'

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {AddVariableModalComponent} from 'src/add-variable-modal/add-variable-modal.component';
import {FormsModule} from '@angular/forms'

import {AngularFontAwesomeModule} from 'angular-font-awesome';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';

import {HttpClientModule} from '@angular/common/http';

export let options: Partial<IConfig> | (() => Partial<IConfig>);

@NgModule({
  declarations: [
    AppComponent,
    AddVariableModalComponent,
  ],
  imports: [
    NgbModule,
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    FormsModule,
    AngularFontAwesomeModule,
    NgxMaskModule.forRoot(options)
  ],
  providers: [],
  bootstrap: [AppComponent],
  entryComponents: [
    AddVariableModalComponent
  ]
})
export class AppModule {
}
