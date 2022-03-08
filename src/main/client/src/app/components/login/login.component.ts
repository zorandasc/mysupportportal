import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { HeaderType } from 'src/app/enum/header-type';
import { NotificationType } from 'src/app/enum/notification-type';
import { User } from 'src/app/model/user';
import { AuthenticationService } from 'src/app/services/authentication.service';
import { NotificationService } from 'src/app/services/notification.service';
import { UserService } from 'src/app/services/user.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit, OnDestroy {
  showLoading: boolean = false;
  private subscriptions: Subscription[] = [];

  constructor(
    private router: Router,
    private userService: UserService,
    private authenticationService: AuthenticationService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    //AKO JE KORISNIK VEC LOGOVAN NEMOZE NA LOGIN PAGE
    if (this.authenticationService.isLoggedIn()) {
      this.router.navigateByUrl('/management');
    } else {
      this.router.navigateByUrl('/login');
    }
  }

  public onLogin(user: User) {
    console.log(user);
    this.showLoading = true;
    this.subscriptions.push(
      this.authenticationService.login(user).subscribe(
        (response: HttpResponse<User>) => {
          const token = response.headers.get(HeaderType.JWT_TOKEN);
          this.authenticationService.saveToken(token!);

          this.userService.addUserToLocalCache(response.body!);

          this.router.navigateByUrl('/management');

          this.showLoading = false;
        },
        (errorResponse: HttpErrorResponse) => {
          console.log(errorResponse);
          this.sendErrorNotification(
            NotificationType.ERROR,
            errorResponse.error.message
          );
          this.showLoading = false;
        }
      )
    );
  }
  private sendErrorNotification(
    notificationType: NotificationType,
    message: any
  ) {
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
