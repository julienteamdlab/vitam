import {Injectable} from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import 'rxjs/add/operator/map';
import {ResourcesService} from '../common/resources.service';
import {BehaviorSubject} from "rxjs/BehaviorSubject"
import {TenantService} from "../common/tenant.service";


const LOGGED_IN = 'REC-loggedIn';
const XSRF_TOKEN = 'REC-XSRF-TOKEN';

export class UserInformation {
  tokenCSRF : string;
}

@Injectable()
export class AuthenticationService {

  private loginState : BehaviorSubject<boolean>;

  constructor(private resourceService: ResourcesService, private cookies: CookieService,
              private tenantService: TenantService, private resourcesService: ResourcesService) {

    let loggedIn = localStorage.getItem(LOGGED_IN) === 'true';
    this.loginState =  new BehaviorSubject<boolean>(loggedIn);
  }

  getSecureMode() {
    return this.resourceService.get('securemode', undefined, 'text');
  }

  verifyAuthentication() {
    return this.resourceService.getTenants();
  }


  loggedIn(user?: UserInformation) {
    localStorage.setItem(LOGGED_IN, 'true');
    if (user !== null && user.tokenCSRF !== null) {
        localStorage.setItem(XSRF_TOKEN, user.tokenCSRF);
    }
    this.loginState.next(true);
  }

  logIn(id: string, password: string) {
    this.resourceService.setTenant("");
    this.tenantService.changeState(this.resourcesService.getTenant());
    return this.resourceService.post('login', null, {
      "token": {"principal": id, "credentials": password}
    });
  }

  loggedOut() {
    this.resourceService.setTenant("");
    this.tenantService.changeState(this.resourcesService.getTenant());
    localStorage.setItem(LOGGED_IN, 'false');
    localStorage.removeItem(XSRF_TOKEN);
    this.loginState.next(false);
  }

  logOut() {
    return this.resourceService.post('logout', null, {});
  }

  getState(): BehaviorSubject<boolean> {
    return this.loginState;
  }
}
