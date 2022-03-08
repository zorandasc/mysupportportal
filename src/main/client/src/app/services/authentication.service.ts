import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { JwtHelperService } from '@auth0/angular-jwt';

import { User } from '../model/user';

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {
  //private host = environment.apiUrl;
  //posto koristimo proxy
  private token: any = null;
  private loggedInUsername: any = null;
  jwtHelper = new JwtHelperService();

  constructor(private http: HttpClient) {}

  //HttpResponse<User> posto vraca i logovanog usera u bady
  //i token u headeru
  public login(user: User): Observable<HttpResponse<User>> {
    return this.http.post<User>(`/user/login`, user, { observe: 'response' });
  }

  //zanima nas samo body
  public register(user: User): Observable<User> {
    return this.http.post<User>(`/user/register`, user);
  }

  public logOut(): void {
    this.token = null;
    this.loggedInUsername = null;
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('users');
  }

  

  public saveToken(token: string): void {
    this.token = token;
    localStorage.setItem('token', token);
  }

 

  public loadToken(): void {
    this.token = localStorage.getItem('token');
  }

  public getToken(): string {
    return this.token;
  }

  public isLoggedIn(): boolean {
    this.loadToken();
    if (this.token != null && this.token !== '') {
      if (this.jwtHelper.decodeToken(this.token).sub != null || '') {
        if (!this.jwtHelper.isTokenExpired(this.token)) {
          this.loggedInUsername = this.jwtHelper.decodeToken(this.token).sub;
          return true;
        }
      }
    }
    this.logOut();
    return false;
  }
}
