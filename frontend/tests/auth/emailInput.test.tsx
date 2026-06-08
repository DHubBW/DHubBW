import { render, screen, fireEvent } from '@testing-library/react-native';
import React from 'react';

import {
  EmailInput,
  DHBW_MAIL_DOMAIN,
  extractLocalPart,
  buildFullEmail,
} from '@/components/auth/EmailInput';

// ---------------------------------------------------------------------------
// Unit tests for helper functions
// ---------------------------------------------------------------------------

describe('extractLocalPart', () => {
  it('strips the DHBW domain from a full email', () => {
    expect(extractLocalPart('max.muster.tinf24b2@student.dhbw-karlsruhe.de')).toBe(
      'max.muster.tinf24b2',
    );
  });

  it('returns value unchanged if it contains a different @-domain', () => {
    expect(extractLocalPart('user@other.com')).toBe('user@other.com');
  });

  it('returns value unchanged if no @ is present', () => {
    expect(extractLocalPart('max.muster.tinf24b2')).toBe('max.muster.tinf24b2');
  });

  it('handles empty string', () => {
    expect(extractLocalPart('')).toBe('');
  });

  it('is case-insensitive for domain matching', () => {
    expect(
      extractLocalPart('max.muster.tinf24b2@Student.DHBW-Karlsruhe.DE'),
    ).toBe('max.muster.tinf24b2');
  });
});

describe('buildFullEmail', () => {
  it('appends DHBW domain to a local part without @', () => {
    expect(buildFullEmail('max.muster.tinf24b2')).toBe(
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
  });

  it('returns value as-is if it already contains @', () => {
    expect(buildFullEmail('max.muster.tinf24b2@student.dhbw-karlsruhe.de')).toBe(
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
  });

  it('returns value as-is for other @ domains', () => {
    expect(buildFullEmail('user@other.com')).toBe('user@other.com');
  });

  it('handles empty string by appending domain', () => {
    expect(buildFullEmail('')).toBe(DHBW_MAIL_DOMAIN);
  });
});

// ---------------------------------------------------------------------------
// Component rendering tests
// ---------------------------------------------------------------------------

describe('EmailInput', () => {
  const mockOnChangeText = jest.fn();

  beforeEach(() => {
    mockOnChangeText.mockClear();
  });

  it('renders the input and domain suffix', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    // The suffix should be visible when no @ is typed
    expect(screen.getByTestId('email-suffix')).toBeTruthy();
    expect(screen.getByTestId('email-suffix').props.children).toBe(DHBW_MAIL_DOMAIN);
  });

  it('shows the local part in the input when value has the DHBW domain', () => {
    render(
      <EmailInput
        value="max.muster.tinf24b2@student.dhbw-karlsruhe.de"
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    const input = screen.getByTestId('email-input');
    expect(input.props.value).toBe('max.muster.tinf24b2');
  });

  it('calls onChangeText with full email when user types local part', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    fireEvent.changeText(screen.getByTestId('email-input'), 'max.muster.tinf24b2');
    expect(mockOnChangeText).toHaveBeenCalledWith(
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
  });

  it('passes full email through when user types/pastes an @ sign', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    fireEvent.changeText(
      screen.getByTestId('email-input'),
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
    expect(mockOnChangeText).toHaveBeenCalledWith(
      'max.muster.tinf24b2@student.dhbw-karlsruhe.de',
    );
  });

  it('hides the suffix when value contains a non-DHBW @ domain', () => {
    render(
      <EmailInput
        value="user@other.com"
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    expect(screen.queryByTestId('email-suffix')).toBeNull();
  });

  it('shows the full value in input when value contains a non-DHBW @ domain', () => {
    render(
      <EmailInput
        value="user@other.com"
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    const input = screen.getByTestId('email-input');
    expect(input.props.value).toBe('user@other.com');
  });

  it('shows suffix again when the full DHBW email is the value', () => {
    render(
      <EmailInput
        value="max.muster.tinf24b2@student.dhbw-karlsruhe.de"
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    // Suffix should show because the local part doesn't contain @
    expect(screen.getByTestId('email-suffix')).toBeTruthy();
  });

  it('calls onBlur when input loses focus', () => {
    const mockBlur = jest.fn();
    render(
      <EmailInput
        value="max.muster.tinf24b2@student.dhbw-karlsruhe.de"
        onChangeText={mockOnChangeText}
        onBlur={mockBlur}
        testID="email"
      />,
    );
    fireEvent(screen.getByTestId('email-input'), 'blur');
    expect(mockBlur).toHaveBeenCalledTimes(1);
  });

  it('disables the input when editable is false', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        editable={false}
        testID="email"
      />,
    );
    expect(screen.getByTestId('email-input').props.editable).toBe(false);
  });

  it('sets correct accessibility label', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        accessibilityLabel="DHBW-Mail"
        testID="email"
      />,
    );
    expect(screen.getByLabelText('DHBW-Mail')).toBeTruthy();
  });

  it('has correct keyboard type settings', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    const input = screen.getByTestId('email-input');
    expect(input.props.keyboardType).toBe('email-address');
    expect(input.props.autoCapitalize).toBe('none');
    expect(input.props.autoCorrect).toBe(false);
  });

  it('uses correct placeholder text', () => {
    render(
      <EmailInput
        value=""
        onChangeText={mockOnChangeText}
        testID="email"
      />,
    );
    const input = screen.getByTestId('email-input');
    expect(input.props.placeholder).toBe('nachname.vorname.a24');
  });
});
