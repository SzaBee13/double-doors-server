import Button from '@site/src/components/Button';
import type {ReactNode} from 'react';

/** Final call-to-action banner. */
export default function CtaSection(): ReactNode {
  return (
    <section className="dd-cta">
      <div className="dd-container dd-cta__inner">
        <h2 className="dd-cta__title">Ready for doors that just work?</h2>
        <p className="dd-cta__description">
          Grab the latest release, drop it in your plugins folder, and open a door.
        </p>
        <div className="dd-cta__actions">
          <Button size="large" to="/download">
            Download now
          </Button>
          <Button variant="secondary" size="large" to="/releases">
            Release notes
          </Button>
        </div>
      </div>
    </section>
  );
}
