import type {ReactNode} from 'react';

const COMMANDS: {command: string; description: string}[] = [
  {command: '/doubledoors toggle', description: 'Toggle double-door behavior for yourself'},
  {command: '/doubledoors reload', description: 'Reload the plugin configuration'},
  {command: '/doubledoors locale', description: 'View or set your language (set requires perPlayerLocaleEnabled: true)'},
  {command: '/doubledoors knock-volume', description: 'Set your personal knock sound volume'},
];

/** Command reference section. */
export default function CommandsSection(): ReactNode {
  return (
    <section className="dd-section dd-section--commands">
      <div className="dd-container">
        <p className="dd-section__eyebrow">Commands</p>
        <h2 className="dd-section__title">Simple commands, sane defaults</h2>
        <div className="dd-commands">
          {COMMANDS.map(({command, description}) => (
            <div className="dd-command" key={command}>
              <code className="dd-command__name">{command}</code>
              <span className="dd-command__description">{description}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
