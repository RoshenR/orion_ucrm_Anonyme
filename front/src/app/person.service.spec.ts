import { TestBed } from '@angular/core/testing';

import { PersonService } from './person.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('PersonService', () => {
  let service: PersonService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(PersonService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
