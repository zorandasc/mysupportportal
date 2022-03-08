import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CustomHttpResponse } from '../model/custom-http-response';
import { User } from '../model/user';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  //private host = environment.apiUrl;
  //posto koristimo proxy
  constructor(private http: HttpClient) {}

  public getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`/user/list`);
  }

  public addUser(formData: FormData): Observable<User> {
    return this.http.post<User>(`/user/add`, formData);
  }

  public updateUser(formData: FormData): Observable<User> {
    return this.http.put<User>(`/user/update`, formData);
  }

  public resetPassword(email: string): Observable<CustomHttpResponse> {
    return this.http.get<CustomHttpResponse>(`/user/resetPassword/${email}`);
  }

  public updateProfileImage(formData: FormData): Observable<HttpEvent<User>> {
    return this.http.post<User>(`/user/updateProfileImage`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  public deleteUser(username: string): Observable<CustomHttpResponse> {
    return this.http.delete<CustomHttpResponse>(`/user/delete/${username}`);
  }

  public addUsersToLocalCache(users: User[]): void {
    localStorage.setItem('users', JSON.stringify(users));
  }

  public addUserToLocalCache(user: User): void {
    localStorage.setItem('user', JSON.stringify(user));
  }

  public getUserFromLocalCache(): User {
    return JSON.parse(localStorage.getItem('user') || '{}');
  }

  public getUsersFromLocalCache(): User[] {
    const users = localStorage.getItem('users');
    if (users) {
      return JSON.parse(users);
    }
    return [];
  }

  public removeUsersFromCache() {
    localStorage.removeItem('users');
  }

  public createUserFormData(
    loggedInUsername: string | null,
    user: User,
    profileImage: File | undefined
  ): FormData {
    const formData = new FormData();
    formData.append('currentUsername', loggedInUsername ?? '');
    formData.append('firstName', user.firstName);
    formData.append('lastName', user.lastName);
    formData.append('username', user.username);
    formData.append('email', user.email);
    formData.append('role', user.role);
    formData.append('profileImage', profileImage ?? '');
    formData.append('isActive', JSON.stringify(user.active));
    formData.append('isNonLocked', JSON.stringify(user.notLocked));

    return formData;
  }
}
