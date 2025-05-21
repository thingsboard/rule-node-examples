import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent, RuleNodeDefinition } from '@shared/models/rule-node.models';
import { Subscription } from 'rxjs';
import { RULE_NODE_CONFIG_TOKEN } from '@shared/components/tokens';
import { TbNodeConfiguration } from '@shared/models/config.models'; // Assuming this is the correct import for TbNodeConfiguration

// Define the configuration structure, mirroring ChangeLabelNodeConfiguration.java
interface ChangeLabelNodeConfig {
  labelSource: 'STATIC' | 'MESSAGE_METADATA' | 'MESSAGE_DATA';
  staticLabelValue: string;
  labelNameOrPattern: string;
  targetEntityType: string; // Ensure this matches the Java class
}

@Component({
  selector: 'tb-action-node-change-label-config', // This will be our configDirective
  templateUrl: './change-label-node-config.html', // Correct path to the HTML
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: ChangeLabelNodeConfigComponent,
      multi: true
    }
  ]
})
export class ChangeLabelNodeConfigComponent extends RuleNodeConfigurationComponent implements OnInit, OnDestroy, ControlValueAccessor {

  changeLabelNodeConfigForm: UntypedFormGroup;
  private valueChangeSubscription: Subscription;
  private propagateChange = (v: any) => { };

  constructor(@Inject(RULE_NODE_CONFIG_TOKEN) protected configuration: RuleNodeConfiguration<ChangeLabelNodeConfig>,
              private fb: UntypedFormBuilder) {
    super(configuration);
  }

  ngOnInit(): void {
    this.changeLabelNodeConfigForm = this.fb.group({
      labelSource: [null, [Validators.required]],
      staticLabelValue: [null],
      labelNameOrPattern: [null],
      targetEntityType: [null] // Added to form
    });

    // Initial update from configuration
    this.updateForm(this.configuration.configuration);

    this.valueChangeSubscription = this.changeLabelNodeConfigForm.valueChanges.subscribe(
      (value) => {
        this.updateConfiguration(value);
        this.propagateChange(this.configuration);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
  }

  writeValue(obj: RuleNodeConfiguration<ChangeLabelNodeConfig>): void {
    if (obj) {
      this.configuration = obj;
      this.updateForm(obj.configuration);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    // We don't use onTouched in this component
  }

  setDisabledState?(isDisabled: boolean): void {
    if (isDisabled) {
      this.changeLabelNodeConfigForm.disable({emitEvent: false});
    } else {
      this.changeLabelNodeConfigForm.enable({emitEvent: false});
    }
  }

  protected updateForm(config: ChangeLabelNodeConfig): void {
    if (this.changeLabelNodeConfigForm && config) {
      this.changeLabelNodeConfigForm.patchValue(config, {emitEvent: false});
      this.updateValidators(config.labelSource);
    }
  }

  protected updateConfiguration(value: ChangeLabelNodeConfig): void {
    if (this.configuration && this.configuration.configuration && value) {
      this.configuration.configuration = {...this.configuration.configuration, ...value};
      this.updateValidators(value.labelSource);
    }
  }

  private updateValidators(labelSource: string): void {
    if (labelSource === 'STATIC') {
      this.changeLabelNodeConfigForm.get('staticLabelValue').setValidators([Validators.required]);
      this.changeLabelNodeConfigForm.get('labelNameOrPattern').clearValidators();
    } else if (labelSource === 'MESSAGE_METADATA' || labelSource === 'MESSAGE_DATA') {
      this.changeLabelNodeConfigForm.get('staticLabelValue').clearValidators();
      this.changeLabelNodeConfigForm.get('labelNameOrPattern').setValidators([Validators.required]);
    } else {
      this.changeLabelNodeConfigForm.get('staticLabelValue').clearValidators();
      this.changeLabelNodeConfigForm.get('labelNameOrPattern').clearValidators();
    }
    this.changeLabelNodeConfigForm.get('staticLabelValue').updateValueAndValidity({emitEvent: false});
    this.changeLabelNodeConfigForm.get('labelNameOrPattern').updateValueAndValidity({emitEvent: false});
  }

  // This method is part of RuleNodeConfigurationComponent and is called by the Rule Chain UI
  // when the configuration is initially loaded or when the user re-opens the dialog.
  // It's responsible for populating the form with the current configuration.
  protected onConfigurationSet(configuration: ChangeLabelNodeConfig) {
    this.updateForm(configuration);
  }
}
