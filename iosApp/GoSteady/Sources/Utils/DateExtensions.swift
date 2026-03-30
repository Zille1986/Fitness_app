import Foundation

extension Date {

    // MARK: - Start / End of Periods

    var startOfDay: Date {
        Calendar.current.startOfDay(for: self)
    }

    var endOfDay: Date {
        Calendar.current.date(byAdding: DateComponents(day: 1, second: -1), to: startOfDay) ?? self
    }

    var startOfWeek: Date {
        let cal = Calendar.current
        let components = cal.dateComponents([.yearForWeekOfYear, .weekOfYear], from: self)
        return cal.date(from: components) ?? self
    }

    var endOfWeek: Date {
        Calendar.current.date(byAdding: .day, value: 6, to: startOfWeek)?.endOfDay ?? self
    }

    var startOfMonth: Date {
        let cal = Calendar.current
        let components = cal.dateComponents([.year, .month], from: self)
        return cal.date(from: components) ?? self
    }

    var endOfMonth: Date {
        Calendar.current.date(byAdding: DateComponents(month: 1, second: -1), to: startOfMonth) ?? self
    }

    // MARK: - Week / Month Range Helpers

    /// Returns (startOfWeek, endOfWeek) for the current week.
    static var thisWeekRange: (start: Date, end: Date) {
        let now = Date()
        return (now.startOfWeek, now.endOfWeek)
    }

    /// Returns (startOfMonth, endOfMonth) for the current month.
    static var thisMonthRange: (start: Date, end: Date) {
        let now = Date()
        return (now.startOfMonth, now.endOfMonth)
    }

    /// Returns (startOfWeek, endOfWeek) for `weeksAgo` weeks in the past.
    static func weekRange(weeksAgo: Int) -> (start: Date, end: Date) {
        let cal = Calendar.current
        guard let targetDate = cal.date(byAdding: .weekOfYear, value: -weeksAgo, to: Date()) else {
            return thisWeekRange
        }
        return (targetDate.startOfWeek, targetDate.endOfWeek)
    }

    /// Returns (startOfMonth, endOfMonth) for `monthsAgo` months in the past.
    static func monthRange(monthsAgo: Int) -> (start: Date, end: Date) {
        let cal = Calendar.current
        guard let targetDate = cal.date(byAdding: .month, value: -monthsAgo, to: Date()) else {
            return thisMonthRange
        }
        return (targetDate.startOfMonth, targetDate.endOfMonth)
    }

    // MARK: - Formatting

    func formatted(as style: DateFormattingStyle) -> String {
        let formatter = DateFormatter()
        switch style {
        case .short:
            formatter.dateStyle = .short
            formatter.timeStyle = .none
        case .medium:
            formatter.dateStyle = .medium
            formatter.timeStyle = .none
        case .long:
            formatter.dateStyle = .long
            formatter.timeStyle = .none
        case .time:
            formatter.dateStyle = .none
            formatter.timeStyle = .short
        case .dateTime:
            formatter.dateStyle = .medium
            formatter.timeStyle = .short
        case .dayMonth:
            formatter.dateFormat = "d MMM"
        case .dayMonthYear:
            formatter.dateFormat = "d MMM yyyy"
        case .weekday:
            formatter.dateFormat = "EEEE"
        case .weekdayShort:
            formatter.dateFormat = "EEE"
        case .monthYear:
            formatter.dateFormat = "MMMM yyyy"
        case .iso8601:
            formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        }
        return formatter.string(from: self)
    }

    // MARK: - Relative Date

    var relativeString: String {
        let cal = Calendar.current
        let now = Date()

        if cal.isDateInToday(self) {
            return "Today"
        } else if cal.isDateInYesterday(self) {
            return "Yesterday"
        } else if cal.isDateInTomorrow(self) {
            return "Tomorrow"
        }

        let daysDiff = cal.dateComponents([.day], from: startOfDay, to: now.startOfDay).day ?? 0

        if daysDiff > 0 && daysDiff < 7 {
            return "\(daysDiff) days ago"
        } else if daysDiff >= 7 && daysDiff < 14 {
            return "1 week ago"
        } else if daysDiff >= 14 && daysDiff < 30 {
            let weeks = daysDiff / 7
            return "\(weeks) weeks ago"
        } else if daysDiff >= 30 && daysDiff < 60 {
            return "1 month ago"
        } else if daysDiff >= 60 && daysDiff < 365 {
            let months = daysDiff / 30
            return "\(months) months ago"
        } else if daysDiff >= 365 {
            let years = daysDiff / 365
            return years == 1 ? "1 year ago" : "\(years) years ago"
        }

        // Future dates
        let futureDays = cal.dateComponents([.day], from: now.startOfDay, to: startOfDay).day ?? 0
        if futureDays > 0 && futureDays < 7 {
            return "in \(futureDays) days"
        } else if futureDays >= 7 && futureDays < 14 {
            return "in 1 week"
        }

        return formatted(as: .dayMonthYear)
    }

    // MARK: - Convenience

    /// Days between this date and another date.
    func daysBetween(_ other: Date) -> Int {
        abs(Calendar.current.dateComponents([.day], from: startOfDay, to: other.startOfDay).day ?? 0)
    }

    /// Returns true if this date is in the same week as another date.
    func isInSameWeek(as other: Date) -> Bool {
        Calendar.current.isDate(self, equalTo: other, toGranularity: .weekOfYear)
    }

    /// Returns true if this date is in the same month as another date.
    func isInSameMonth(as other: Date) -> Bool {
        Calendar.current.isDate(self, equalTo: other, toGranularity: .month)
    }

    /// Day of week (1 = Sunday, 2 = Monday, ... 7 = Saturday).
    var dayOfWeek: Int {
        Calendar.current.component(.weekday, from: self)
    }

    /// Milliseconds since 1970 (for interop with Android timestamps).
    var millisecondsSince1970: Int64 {
        Int64(timeIntervalSince1970 * 1000)
    }

    /// Create a Date from milliseconds since 1970.
    static func fromMilliseconds(_ ms: Int64) -> Date {
        Date(timeIntervalSince1970: Double(ms) / 1000.0)
    }
}

enum DateFormattingStyle {
    case short          // 28/3/26
    case medium         // 28 Mar 2026
    case long           // 28 March 2026
    case time           // 2:30 PM
    case dateTime       // 28 Mar 2026, 2:30 PM
    case dayMonth       // 28 Mar
    case dayMonthYear   // 28 Mar 2026
    case weekday        // Saturday
    case weekdayShort   // Sat
    case monthYear      // March 2026
    case iso8601        // 2026-03-28T14:30:00+1100
}
