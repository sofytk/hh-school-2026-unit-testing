package ru.hh.school.unittesting.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.school.unittesting.homework.LibraryManager;
import ru.hh.school.unittesting.homework.NotificationService;
import ru.hh.school.unittesting.homework.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  private static final String BOOK_ID = "bookId";
  private static final String USER_ID = "userId";
  private static final String OTHER_USER_ID = "otherUserId";

  @Mock
  private UserService userService;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private LibraryManager libraryManager;

  @Test
  void addBookFirstTime() {
    libraryManager.addBook(BOOK_ID, 4);
    assertEquals(4, libraryManager.getAvailableCopies(BOOK_ID));
  }

  @Test
  void addBookTwice() {
    libraryManager.addBook(BOOK_ID, 4);
    libraryManager.addBook(BOOK_ID, 5);
    assertEquals(9, libraryManager.getAvailableCopies(BOOK_ID));
  }

  @Test
  void borrowBookUserNotActive() {
    when(userService.isUserActive(USER_ID)).thenReturn(false);
    assertFalse(libraryManager.borrowBook(BOOK_ID, USER_ID));
    verify(notificationService).notifyUser(USER_ID, "Your account is not active.");
  }

  @Test
  void borrowBookNoAvailableCopies() {
    when(userService.isUserActive(USER_ID)).thenReturn(true);
    assertFalse(libraryManager.borrowBook(BOOK_ID, USER_ID));
  }

  @Test
  void borrowBookSuccess() {
    when(userService.isUserActive(USER_ID)).thenReturn(true);
    libraryManager.addBook(BOOK_ID, 5);

    assertTrue(libraryManager.borrowBook(BOOK_ID, USER_ID));
    assertEquals(4, libraryManager.getAvailableCopies(BOOK_ID));
    verify(notificationService).notifyUser(USER_ID, "You have borrowed the book: " + BOOK_ID);
  }

  @Test
  void returnBookNotBorrowed() {
    assertFalse(libraryManager.returnBook(BOOK_ID, USER_ID));
  }

  @Test
  void returnBookWrongUser() {
    when(userService.isUserActive(USER_ID)).thenReturn(true);
    libraryManager.addBook(BOOK_ID, 1);
    libraryManager.borrowBook(BOOK_ID, USER_ID);
    assertFalse(libraryManager.returnBook(BOOK_ID, OTHER_USER_ID));
  }

  @Test
  void returnBookSuccess() {
    when(userService.isUserActive(USER_ID)).thenReturn(true);
    libraryManager.addBook(BOOK_ID, 1);
    libraryManager.borrowBook(BOOK_ID, USER_ID);

    assertTrue(libraryManager.returnBook(BOOK_ID, USER_ID));
    assertEquals(1, libraryManager.getAvailableCopies(BOOK_ID));
    verify(notificationService)
        .notifyUser(USER_ID, "You have returned the book: " + BOOK_ID);
  }

  @Test
  void calculateDynamicLateFeeNegativeDays() {
    assertThrows(IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false));
  }

  @ParameterizedTest
  @CsvSource({
      "4, false, false, 2.00",
      "4, true,  false, 3.00",
      "4, false, true,  1.60",
      "4, true,  true,  2.40"
  })
  void calculateDynamicLateFeeParameterized(int days, boolean bestseller, boolean premium, double expected) {
    assertEquals(expected, libraryManager.calculateDynamicLateFee(days, bestseller, premium));
  }
}
