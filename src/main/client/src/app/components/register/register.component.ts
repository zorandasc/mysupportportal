import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { NotificationType } from 'src/app/enum/notification-type';
import { User } from 'src/app/model/user';
import { AuthenticationService } from 'src/app/services/authentication.service';
import { NotificationService } from 'src/app/services/notification.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
})
export class RegisterComponent implements OnInit, OnDestroy {
  showLoading: boolean = false;
  private subscriptions: Subscription[] = [];

  constructor(
    private router: Router,
    private authenticationService: AuthenticationService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    //AKO JE KORISNIK VEC LOGOVAN
    if (this.authenticationService.isLoggedIn()) {
      this.router.navigateByUrl('/user/management');
    }
  }

  public onRegister(user: User) {
    console.log(user);
    this.showLoading = true;
    this.subscriptions.push(
      this.authenticationService.register(user).subscribe(
        (response: User) => {
          this.sendNotification(
            NotificationType.SUCCESS,
            `A new account created for ${response.firstName}. 
            Please check your email ${response.email} for password login.`
          );

          this.showLoading = false;
        },
        (errorResponse: HttpErrorResponse) => {
          console.log(errorResponse);
          this.sendNotification(
            NotificationType.ERROR,
            errorResponse.error.message
          );
          this.showLoading = false;
        }
      )
    );
  }

  private sendNotification(notificationType: NotificationType, message: any) {
    if (message) {
      this.notificationService.notify(notificationType, message);
    } else {
      this.notificationService.notify(notificationType, 'An Error Ocurrred.');
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
  }
}
