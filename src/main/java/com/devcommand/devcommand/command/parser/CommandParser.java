package com.devcommand.devcommand.command.parser;

import com.devcommand.devcommand.command.Command;

/**
 * Converts external input (e.g. natural language, WhatsApp message) into a Command.
 * No AI or NLP is implemented yet.
 */
public interface CommandParser<T> {
    Command parse(T input, Long authenticatedUserId);
}
