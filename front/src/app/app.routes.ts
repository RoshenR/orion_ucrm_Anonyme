import { Routes } from '@angular/router';
import { PersonDetailsComponent } from './person-details/person-details.component';
import { MainDashboardComponent } from './main-dashboard/main-dashboard.component';
import { OrganizationDetailsComponent } from './organization-details/organization-details.component';

export const routes: Routes = [
    { path: '', component: MainDashboardComponent },
    { path: 'persons/:personId', component: PersonDetailsComponent },
    { path: 'organizations/:orgId', component: OrganizationDetailsComponent },
    { path: '**', redirectTo: '' }
];
