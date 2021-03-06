import {Component, Input, OnInit} from '@angular/core';
import { ManagementModel } from './management-model';
import {ComputeRulesUtilsService} from './compute-rules-utils.service';

@Component({
  selector: 'vitam-archive-rule-bloc',
  templateUrl: './archive-rule-bloc.component.html'
})
export class ArchiveRuleBlocComponent implements OnInit {
  @Input() unitId: string;
  @Input() data: any;

  update = false;

  computedData: ManagementModel;
  unitProperties: any;

  constructor(private computeRulesUtilsService: ComputeRulesUtilsService) { }

  ngOnInit() {
    this.init();
  }

  init() {
    const computedInfo = this.computeRulesUtilsService.computeEffectiveRules(this.data['#management'],
      this.data.InheritedRules, this.data.ArchiveUnitProfile);

    this.computedData = computedInfo.managementInfo;
    this.unitProperties = computedInfo.unitProperties;
  }

  switchUpdate(response) {
    this.update = !this.update;

    if(response) {
      this.data = response;
      this.init();
    }
  }
}
